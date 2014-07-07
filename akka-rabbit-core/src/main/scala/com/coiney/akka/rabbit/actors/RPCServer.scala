package com.coiney.akka.rabbit.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, Actor, ActorRef, Props}
import com.coiney.akka.rabbit.messages.Request
import com.rabbitmq.client.{Channel, DefaultConsumer}

import scala.concurrent.duration._

import com.coiney.akka.rabbit.{ExchangeConfig, QueueConfig, ChannelConfig}
import com.coiney.akka.rabbit.RPC._


object RPCServer {
  def apply(processor: Processor, channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): RPCServer =
    new RPCServer(processor, channelConfig, provision) with AMQPRabbitFunctions

  def props(processor: Processor, channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): Props =
    Props(RPCServer(processor, channelConfig, provision))
}

class RPCServer(processor: Processor,
                channelConfig: Option[ChannelConfig] = None,
                provision: Seq[Request] = Seq.empty[Request]) extends ChannelKeeper(channelConfig, provision) {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.messages._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef): Actor.Receive = rpcServerConnected(channel) orElse super.connected(channel, handler)

  def rpcServerConnected(channel: Channel): Actor.Receive = {
    case req @ ConsumeQueue(queueConfig) =>
      queueConsume(channel)(queueConfig, autoAck = false, consumer.get)

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
