package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorRef, Props}
import com.rabbitmq.client.{DefaultConsumer, Channel}

import com.coiney.akka.rabbit.protocol.RabbitRequest
import com.coiney.akka.rabbit.ChannelConfig


object Consumer {
  def apply(listener: ActorRef, autoAck: Boolean = false, channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): Consumer =
    new Consumer(listener, autoAck, channelConfig, provision) with AMQPRabbitFunctions with RequestHandler

  def props(listener: ActorRef, autoAck: Boolean = false, channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): Props =
    Props(Consumer(listener, autoAck, channelConfig, provision))
}


class Consumer(listener: ActorRef,
               autoAck: Boolean = false,
               channelConfig: Option[ChannelConfig] = None,
               provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]) extends ChannelKeeper(channelConfig, provision) {
  this: RabbitFunctions with RequestHandler =>
  import com.coiney.akka.rabbit.protocol._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef) = consumerConnected(channel, handler) orElse super.connected(channel, handler)

  def consumerConnected(channel: Channel, handler: ActorRef): Actor.Receive = {
    case req @ ConsumeQueue(queueConfig) => consumer match {
      case None    => log.debug("No consumer registered")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          val consumerTag = queueConsume(channel)(queueConfig, autoAck, c)
          log.debug(s"Consuming using $consumerTag.")
          consumerTag
        }
    }

    case req @ CancelConsume(consumerTag) => consumer match {
      case None    => log.debug("Channel is not a consumer")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          basicCancel(channel)(consumerTag)
        }
    }
  }

  override def onChannel(channel: Channel): Unit = {
    super.onChannel(channel)
    consumer = Some(addConsumer(channel)(listener, self))
  }

}
