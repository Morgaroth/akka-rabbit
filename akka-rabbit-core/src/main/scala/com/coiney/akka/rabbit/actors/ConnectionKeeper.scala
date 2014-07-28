package com.coiney.akka.rabbit.actors

import akka.actor._
import com.coiney.akka.rabbit.RabbitSystem
import com.rabbitmq.client._

import com.coiney.akka.pattern.WatchingObservable

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


object ConnectionKeeper {
  case object Connect
  case object GetChannel
  case class CreateChild(props: Props, name: Option[String])

  sealed trait State
  case object Connected extends State
  case object Disconnected extends State

  def apply(settings: RabbitSystem.Settings): ConnectionKeeper =
    new ConnectionKeeper(settings) with AMQPRabbitFunctions with RabbitConnectionProvider

  def props(settings: RabbitSystem.Settings): Props =
    Props(ConnectionKeeper(settings))
}


class ConnectionKeeper(protected val settings: RabbitSystem.Settings) extends Actor
                                                                      with WatchingObservable
                                                                      with RabbitConfiguration
                                                                      with ActorLogging {
  this: RabbitConnectionProvider with RabbitFunctions =>
  import com.coiney.akka.rabbit.protocol.HandleShutdown
  import com.coiney.akka.rabbit.actors.ConnectionKeeper._

  implicit val ec = context.dispatcher

  var connection: Option[Connection] = None
  var connectionHeartbeat: Option[Cancellable] = None

  override def preStart(): Unit = {
    scheduleConnect()
  }

  override def postStop(): Unit = {
    connection.foreach(c => try closeConnection(c))
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case Terminated(dead) if observers.contains(dead) => deregisterObserver(dead, None)
      case _                                            => super.unhandled(message)
    }
  }

  override def receive: Actor.Receive = observeReceive(None, None) orElse disconnected

  def disconnected: Actor.Receive = {
    case Connect =>
      log.debug(s"Trying to connect with $safeConnectionUri")
      Try(createObservedConnection()) match {
        case Success(newConnection) =>
          log.info(s"Connected to $safeConnectionUri")
          sendEvent(Connected)
          connection.foreach(c => try closeConnection(c))
          connection = Some(newConnection)
          connectionHeartbeat.foreach(c => c.cancel())
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
      Try(createChannel(conn)) match {
        case Success(channel) =>
          log.debug(s"Channel created [$channel]")
          sender ! ChannelKeeper.HandleChannel(channel)
        case Failure(cause) =>
          log.error(cause, "Channel creation failed")
          connection = None
          scheduleConnect()
          context.become(observeReceive(None, None) orElse disconnected)
      }

    case CreateChild(props, name) =>
      val child = createChild(props, name)
      sender ! child

    case HandleShutdown(cause) =>
      log.error(cause, s"The AMQP connection to $safeConnectionUri was lost")
      connection = None
      sendEvent(Disconnected)
      scheduleConnect()
      context.become(observeReceive(None, None) orElse disconnected)
  }

  private def createObservedConnection(): Connection = {
    val newConnection = createConnection()
    addShutdownListener(newConnection)(self)
    newConnection
  }

  private def createChild(props: Props, name: Option[String]) = {
    name match {
      case None    => context.actorOf(props)
      case Some(n) => context.actorOf(props, n)
    }
  }

  private def scheduleConnect(): Unit = {
    connectionHeartbeat = Some(context.system.scheduler.schedule(100.millis, 10000.millis, self, Connect))
  }

}
