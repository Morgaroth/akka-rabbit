package com.coiney.akka.rabbit.actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, Actor, ActorRef, Props}
import com.coiney.akka.rabbit.protocol.RabbitRequest
import com.rabbitmq.client.{Channel, DefaultConsumer}

import scala.concurrent.duration._

import com.coiney.akka.rabbit.ChannelConfig
import com.coiney.akka.rabbit.protocol._


object RPCServer {
  def apply(processor: RabbitRPCProcessor, channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): RPCServer =
    new RPCServer(processor, channelConfig, provision) with AMQPRabbitFunctions

  def props(processor: RabbitRPCProcessor, channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): Props =
    Props(RPCServer(processor, channelConfig, provision))
}

class RPCServer(processor: RabbitRPCProcessor,
                channelConfig: Option[ChannelConfig] = None,
                provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]) extends ChannelKeeper(channelConfig, provision) {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.protocol._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef): Actor.Receive = rpcServerConnected(channel) orElse super.connected(channel, handler)

  def rpcServerConnected(channel: Channel): Actor.Receive = {
    case req @ ConsumeQueue(queueConfig) =>
      queueConsume(channel)(queueConfig, autoAck = false, consumer.get)

    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      val rpcProcessor = context.actorOf(RPCProcessor.props(processor, channel))
      rpcProcessor forward hd
  }

  override def onChannel(channel: Channel): Unit = {
    super.onChannel(channel)
    consumer = Some(addConsumer(channel)(self, self))
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 1, withinTimeRange = 1.minute){
      case _ => Stop
    }

}
