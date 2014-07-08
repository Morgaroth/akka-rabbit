package com.coiney.akka.rabbit.actors

import akka.actor.{ActorRef, Actor, Props}
import com.rabbitmq.client.{AMQP, DefaultConsumer, Channel}

import com.coiney.akka.rabbit.messages.{Request, HandleDelivery}
import com.coiney.akka.rabbit.{QueueConfig, ChannelConfig, RPC}


object RPCClient {
  case class PendingRequest(sender: ActorRef, expectedNumberOfResponses: Int, handleDeliveries: List[HandleDelivery])

  def apply(channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): RPCClient =
    new RPCClient(channelConfig, provision) with AMQPRabbitFunctions

  def props(channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): Props =
    Props(RPCClient(channelConfig, provision))
}

class RPCClient(channelConfig: Option[ChannelConfig] = None,
                provision: Seq[Request] = Seq.empty[Request]) extends ChannelKeeper(channelConfig, provision) {
  this: RabbitFunctions =>
  import RPCClient._
  import com.coiney.akka.rabbit.messages._

  var queue: Option[String] = None
  var consumer: Option[DefaultConsumer] = None
  var pendingRequests: Map[String, PendingRequest] = Map.empty[String, PendingRequest]

  override def connected(channel: Channel, handler: ActorRef): Actor.Receive = rpcClientConnected(channel) orElse super.connected(channel, handler)

  def rpcClientConnected(channel: Channel): Actor.Receive = {
    case RPC.Request(publishes, numberOfResponses) =>
      val correlationId = java.util.UUID.randomUUID().toString
      publishes.foreach{ p =>
        val props = p.properties.getOrElse(new AMQP.BasicProperties()).builder().correlationId(correlationId).replyTo(queue.get).build()
        basicPublish(channel)(p.exchange, p.routingKey, p.body, p.mandatory, p.immediate, Some(props))
      }
      if (numberOfResponses > 0) {
        pendingRequests += (correlationId -> PendingRequest(sender, numberOfResponses, List.empty[HandleDelivery]))
      }

    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      basicAck(channel)(envelope.getDeliveryTag)
      val correlationId = properties.getCorrelationId
      pendingRequests.get(correlationId) match {
        case None =>
          log.error(s"Unexpected message: [$correlationId] ${new String(body)}")
        case Some(pendingRequest) =>
          pendingRequests = pendingRequests.filterNot(_._1 == correlationId) + (correlationId -> pendingRequest.copy(handleDeliveries = hd :: pendingRequest.handleDeliveries))
          val updatedPendingRequest = pendingRequests.get(correlationId).get
          if (updatedPendingRequest.handleDeliveries.size == updatedPendingRequest.expectedNumberOfResponses) {
            updatedPendingRequest.sender ! RPC.Response(updatedPendingRequest.handleDeliveries)
            pendingRequests = pendingRequests.filterNot(_._1 == correlationId)
          }
      }
  }

  override def channelCallback(channel: Channel): Unit = {
    super.channelCallback(channel)
    createAndConsumeReplyQueue(channel)
  }

  private def createAndConsumeReplyQueue(channel: Channel): Unit = {
    queue = Some(queueDeclare(channel)(QueueConfig("", durable = false, exclusive = true, autoDelete = true, Map.empty[String, AnyRef])).getQueue)
    consumer = Some(addConsumer(channel)(self, self))
    basicConsume(channel)(queue.get, autoAck = false, consumer.get)
  }

}
