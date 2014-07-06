package com.coiney.akka.rabbit.actors

import akka.actor._
import com.rabbitmq.client.Channel


object ChannelKeeper {
  case class HandleChannel(channel: Channel)

  sealed trait State
  case object Connected extends State
  case object Disconnected extends State

  def props(): Props = Props(classOf[ChannelKeeper])
}


private[rabbit] class ChannelKeeper extends Actor
                                    with WatchingObservable
                                    with ActorLogging {
  import com.coiney.akka.rabbit.actors.ChannelKeeper._
  import com.coiney.akka.rabbit.messages._

  override def preStart(): Unit = {
    context.parent ! ConnectionKeeper.GetChannel
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case Terminated(dead) if observers.contains(dead) => deregisterObserver(dead, None)
      case _                                            => super.unhandled(message)
    }
  }

  override def receive: Actor.Receive = observeReceive(None, None) orElse disconnected

  def disconnected: Actor.Receive =  {
    case HandleChannel(channel) =>
      log.debug(s"received channel $channel")
      val handler = context.actorOf(ChannelHandler.props(channel), "handler")
      handler ! AddShutdownListener(self)
      handler ! AddReturnListener(self)
      channelCallback(channel)
      sendEvent(Connected)
      context.become(observeReceive(Some(Connected), None) orElse connected(channel, handler))

    case req: Request =>
      sender ! DisconnectedError(req)
  }

  def connected(channel: Channel, handler: ActorRef): Actor.Receive = {
    case res: Response => ()

    case req: Request =>
      handler forward req

    case HandleShutdown(cause) if !cause.isInitiatedByApplication =>
      log.error(cause, "The AMQP channel was lost")
      context.stop(handler)
      sendEvent(Disconnected)
      context.parent ! ConnectionKeeper.GetChannel
      context.become(observeReceive(None, None) orElse disconnected)
  }

  def channelCallback(channel: Channel): Unit = {}

}
