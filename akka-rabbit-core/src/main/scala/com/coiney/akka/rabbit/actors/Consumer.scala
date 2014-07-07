package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorRef, Props}
import com.rabbitmq.client.{DefaultConsumer, Channel}

import com.coiney.akka.rabbit.messages.Request
import com.coiney.akka.rabbit.{ExchangeConfig, QueueConfig, ChannelConfig}


object Consumer {
  def apply(listener: ActorRef, autoAck: Boolean = false, channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): Consumer =
    new Consumer(listener, autoAck, channelConfig, provision) with AMQPRabbitFunctions with RequestHandler

  def props(listener: ActorRef, autoAck: Boolean = false, channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): Props =
    Props(Consumer(listener, autoAck, channelConfig, provision))
}


class Consumer(listener: ActorRef,
               autoAck: Boolean = false,
               channelConfig: Option[ChannelConfig] = None,
               provision: Seq[Request] = Seq.empty[Request]) extends ChannelKeeper(channelConfig, provision) {
  this: RabbitFunctions with RequestHandler =>
  import com.coiney.akka.rabbit.messages._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef) = consumerConnected(channel, handler) orElse super.connected(channel, handler)

  def consumerConnected(channel: Channel, handler: ActorRef): Actor.Receive = {
    case req @ ConsumeQueue(queueConfig) => consumer match {
      case None    => log.debug("Channel is not a consumer.")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          val consumerTag = queueConsume(channel)(queueConfig, autoAck, c)
          log.debug(s"Consuming using $consumerTag.")
          consumerTag
        }
    }

    case req @ ConsumeBinding(exchangeName, exchangeType, queueName, routingKey, exchangeDurable, exchangeAutoDelete, queueDurable, queueExclusive, queueAutoDelete, exchangeArgs, queueArgs, bindingArgs) => consumer match {
      case None    => log.debug("Channel is not a consumer.")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          exchangeDeclare(channel)(ExchangeConfig(exchangeName, exchangeType, exchangeDurable, exchangeAutoDelete, exchangeArgs))
          queueDeclare(channel)(QueueConfig(queueName, queueDurable, queueExclusive, queueAutoDelete, queueArgs))
          queueBind(channel)(queueName, exchangeName, routingKey, exchangeArgs)
          val consumerTag = basicConsume(channel)(queueName, autoAck, c)
          log.debug(s"Consuming using $consumerTag.")
          consumerTag
        }
    }

    case req @ CancelConsume(consumerTag) => consumer match {
      case None    => log.debug("Channel is not a consumer.")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          basicCancel(channel)(consumerTag)
        }
    }
  }

  override def channelCallback(channel: Channel): Unit = {
    super.channelCallback(channel)
    consumer = Some(addConsumer(channel)(listener))
  }

}
