package com.coiney.akka.rabbit.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, Actor, ActorRef, Props}
import com.rabbitmq.client.{Channel, DefaultConsumer}

import scala.concurrent.duration._

import com.coiney.akka.rabbit.{ExchangeConfig, QueueConfig, ChannelConfig}
import com.coiney.akka.rabbit.RPC._


object RPCServer {
  def apply(processor: Processor, channelConfig: Option[ChannelConfig] = None): RPCServer = new RPCServer(processor, channelConfig) with AMQPRabbitFunctions

  def props(processor: Processor, channelConfig: Option[ChannelConfig] = None): Props = Props(RPCServer(processor, channelConfig))
}

class RPCServer(processor: Processor, channelConfig: Option[ChannelConfig] = None) extends ChannelKeeper(channelConfig) {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.messages._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef): Actor.Receive = rpcServerConnected(channel) orElse super.connected(channel, handler)

  def rpcServerConnected(channel: Channel): Actor.Receive = {
    case req @ ConsumeQueue(queueConfig) =>
      queueConsume(channel)(queueConfig, autoAck = false, consumer.get)

    case req @ ConsumeBinding(exchangeName, exchangeType, queueName, routingKey, exchangeDurable, exchangeAutoDelete, queueDurable, queueExclusive, queueAutoDelete, exchangeArgs, queueArgs, bindingArgs) =>
      exchangeDeclare(channel)(ExchangeConfig(exchangeName, exchangeType, exchangeDurable, exchangeAutoDelete, exchangeArgs))
      queueDeclare(channel)(QueueConfig(queueName, queueDurable, queueExclusive, queueAutoDelete, queueArgs))
      queueBind(channel)(queueName, exchangeName, routingKey, exchangeArgs)
      basicConsume(channel)(queueName, autoAck = false, consumer.get)

    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      val rpcProcessor = context.actorOf(RPCProcessor.props(processor, channel))
      rpcProcessor forward hd
  }

  override def channelCallback(channel: Channel): Unit = {
    super.channelCallback(channel)
    consumer = Some(addConsumer(channel)(self))
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 1, withinTimeRange = 1.minute){
      case _ => Stop
    }

}
