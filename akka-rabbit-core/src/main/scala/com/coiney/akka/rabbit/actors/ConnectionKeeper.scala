package com.coiney.akka.rabbit.actors

import akka.actor._
import com.rabbitmq.client._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


object ConnectionKeeper {
  case object Connect
  case object GetChannel
  private[rabbit] case class CreateChild(props: Props, name: Option[String])

  case class Shutdown(cause: ShutdownSignalException)

  sealed trait State
  case object Connected extends State
  case object Disconnected extends State

  def props(connectionFactory: ConnectionFactory): Props = Props(classOf[ConnectionKeeper], connectionFactory)
}


class ConnectionKeeper(connectionFactory: ConnectionFactory) extends Actor
                                                             with WatchingObservable
                                                             with ActorLogging {
  import com.coiney.akka.rabbit.actors.ConnectionKeeper._
  implicit val ec = context.dispatcher

  var connection: Option[Connection] = None

  context.system.scheduler.schedule(100.millis, 10000.millis, self, Connect)

  override def postStop(): Unit = {
    connection.foreach(c => closeConnection(c))
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case Terminated(dead) if observers.contains(dead) => unregisterObserver(dead, None)
      case _                                            => super.unhandled(message)
    }
  }

  override def receive: Actor.Receive = observeReceive(None, None) orElse disconnected

  def disconnected: Actor.Receive = {
    case Connect =>
//      log.debug(s"Trying to connect with ${AMQP.Connection.fullUri(connectionFactory)}.")
      createConnection() match {
        case Success(newConnection) =>
//          log.info(s"Connected to ${AMQP.Connection.fullUri(connectionFactory)}.")
          sendEvent(Connected)
          connection.foreach(c => closeConnection(c))
          connection = Some(newConnection)
          context.become(observeReceive(Some(Connected), None) orElse connected(newConnection))
        case Failure(cause) =>
          log.error(cause, "Establishing the AMQP connection failed.")
      }

    case CreateChild(props, name) =>
      val child = createChild(props, name)
      sender ! child

  }

  def connected(conn: Connection): Actor.Receive = {
    case Connect => ()

    case GetChannel =>
      createChannel(conn) match {
        case Success(channel) =>
          log.info("channel created")
          sender ! ChannelKeeper.HandleChannel(channel)
        case Failure(cause) =>
          log.error(cause, "Channel creation failed.")
          connection = None
          self ! Connect
          context.become(observeReceive(None, None) orElse disconnected)
      }

    case CreateChild(props, name) =>
      val child = createChild(props, name)
      sender ! child

    case Shutdown(cause) =>
      log.error(cause, "The AMQP connection was lost")
      connection = None
      sendEvent(Disconnected)
      self ! Connect
      context.become(observeReceive(None, None) orElse disconnected)
  }

  private def createConnection(): Try[Connection] = Try {
    val newConnection = connectionFactory.newConnection()
    newConnection.addShutdownListener(new ShutdownListener {
      override def shutdownCompleted(cause: ShutdownSignalException): Unit =
        self ! Shutdown(cause)
    })
    newConnection
  }

  private def closeConnection(connection: Connection): Unit = Try {
    connection.close()
  }

  private def createChannel(connection: Connection): Try[Channel] = Try {
    connection.createChannel()
  }

  private def createChild(props: Props, name: Option[String]) = {
    name match {
      case None    => context.actorOf(props)
      case Some(n) => context.actorOf(props, n)
    }
  }

}
