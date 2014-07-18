package com.coiney.akka.rabbit.actors

import akka.actor._
import com.rabbitmq.client.Channel

import com.coiney.akka.pattern.WatchingObservable
import com.coiney.akka.rabbit.ChannelConfig
import com.coiney.akka.rabbit.protocol.RabbitRequest


object ChannelKeeper {
  case class HandleChannel(channel: Channel)

  sealed trait State
  case object Connected extends State
  case object Disconnected extends State

  def apply(channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): ChannelKeeper =
    new ChannelKeeper(channelConfig, provision) with AMQPRabbitFunctions

  def props(channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): Props =
    Props(ChannelKeeper(channelConfig, provision))
}


private[rabbit] class ChannelKeeper(channelConfig: Option[ChannelConfig] = None,
                                    provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]) extends Actor
                                                                                  with WatchingObservable
                                                                                  with ActorLogging {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.actors.ChannelKeeper._
  import com.coiney.akka.rabbit.protocol._

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
      log.debug(s"Received channel [$channel]")
      val handler = context.actorOf(ChannelHandler.props(channel), "handler")
      handler ! AddShutdownListener(self)
      handler ! AddReturnListener(self)
      channelCallback(channel)
      provision.foreach(request => self ! request)
      sendEvent(Connected)
      context.become(observeReceive(Some(Connected), None) orElse connected(channel, handler))

    case req: RabbitRequest =>
      sender ! DisconnectedError(req)
  }

  def connected(channel: Channel, handler: ActorRef): Actor.Receive = {
    case res: RabbitResponse => ()

    case req: RabbitRequest =>
      handler forward req

    case HandleShutdown(cause) if !cause.isInitiatedByApplication =>
      log.error(cause, s"Lost the AMQP channel [$channel]")
      context.stop(handler)
      sendEvent(Disconnected)
      context.parent ! ConnectionKeeper.GetChannel
      context.become(observeReceive(None, None) orElse disconnected)
  }

  def channelCallback(channel: Channel): Unit = {
    channelConfig.foreach(cfg => configureChannel(channel)(cfg))
  }

}
