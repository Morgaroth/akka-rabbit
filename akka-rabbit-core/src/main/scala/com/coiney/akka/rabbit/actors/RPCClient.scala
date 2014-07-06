package com.coiney.akka.rabbit.actors

import akka.actor.{ActorRef, Actor, Props}
import com.coiney.akka.rabbit.messages.HandleDelivery
import com.coiney.akka.rabbit.RPC
import com.rabbitmq.client.{AMQP, Envelope, DefaultConsumer, Channel}

import scala.collection.JavaConversions._


object RPCClient {
  case class PendingRequest(sender: ActorRef, expectedNumberOfResponses: Int, handleDeliveries: List[HandleDelivery])

  def apply(): RPCClient = new RPCClient()

  def props(): Props = Props(RPCClient())
}

class RPCClient extends ChannelKeeper {
  import RPCClient._
  import com.coiney.akka.rabbit.messages._

  var queue: Option[String] = None
  var consumer: Option[DefaultConsumer] = None
  var pendingRequests: Map[String, PendingRequest] = Map.empty[String, PendingRequest]

  override def connected(channel: Channel, handler: ActorRef): Actor.Receive = rpcClientConnected(channel) orElse super.connected(channel, handler)

  def rpcClientConnected(channel: Channel): Actor.Receive = {
    case RPC.Request(publishes, numberOfResponses) =>
      val correlationId = java.util.UUID.randomUUID().toString
      publishes.foreach{ publish =>
        val props = publish.properties.getOrElse(new AMQP.BasicProperties()).builder().correlationId(correlationId).replyTo(queue.get).build()
        channel.basicPublish(publish.exchange, publish.routingKey, publish.mandatory, publish.immediate, props, publish.body)
      }
      if (numberOfResponses > 0) {
        pendingRequests += (correlationId -> PendingRequest(sender, numberOfResponses, List.empty[HandleDelivery]))
      }

    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      channel.basicAck(envelope.getDeliveryTag, false)
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
    queue = Some(channel.queueDeclare("", false, true, true, Map.empty[String, AnyRef]).getQueue)
    consumer = Some(new DefaultConsumer(channel){
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        self ! HandleDelivery(consumerTag, envelope, properties, body)
    })
    channel.basicConsume(queue.get, false, consumer.get)
  }

}
