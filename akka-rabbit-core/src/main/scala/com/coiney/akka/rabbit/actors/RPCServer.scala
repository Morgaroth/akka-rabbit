package com.coiney.akka.rabbit.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, Actor, ActorRef, Props}
import com.rabbitmq.client.{AMQP, Channel, DefaultConsumer, Envelope}

import scala.collection.JavaConversions._
import scala.concurrent.duration._

import com.coiney.akka.rabbit.RPC._


object RPCServer {
  def apply(processor: Processor): RPCServer = new RPCServer(processor)

  def props(processor: Processor): Props = Props(RPCServer(processor))
}

class RPCServer(processor: Processor) extends ChannelKeeper {
  import com.coiney.akka.rabbit.messages._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef): Actor.Receive = rpcServerConnected(channel) orElse super.connected(channel, handler)

  def rpcServerConnected(channel: Channel): Actor.Receive = {
    case req @ ConsumeQueue(name, durable, exclusive, autoDelete, arguments) =>
      channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)
      channel.basicConsume(name, false, consumer.get)

    case req @ ConsumeBinding(exchangeName, exchangeType, queueName, routingKey, exchangeDurable, exchangeAutoDelete, queueDurable, queueExclusive, queueAutoDelete, exchangeArgs, queueArgs, bindingArgs) =>
      channel.exchangeDeclare(exchangeName, exchangeType, exchangeDurable, exchangeAutoDelete, exchangeArgs)
      channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, queueArgs)
      channel.queueBind(queueName, exchangeName, routingKey, exchangeArgs)
      channel.basicConsume(queueName, false, consumer.get)

    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      val rpcProcessor = context.actorOf(RPCProcessor.props(processor, channel))
      rpcProcessor forward hd
  }

  override def channelCallback(channel: Channel): Unit = {
    super.channelCallback(channel)
    consumer = Some(new DefaultConsumer(channel){
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        self ! HandleDelivery(consumerTag, envelope, properties, body)

      override def handleCancel(consumerTag: String): Unit =
        self ! HandleCancel(consumerTag)
    })
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 1, withinTimeRange = 1.minute){
      case _ => Stop
    }

}
