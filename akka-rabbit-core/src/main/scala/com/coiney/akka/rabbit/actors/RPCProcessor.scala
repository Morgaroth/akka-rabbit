package com.coiney.akka.rabbit.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.rabbitmq.client.{AMQP, Channel}

import com.coiney.akka.rabbit.protocol.RabbitRPCProcessor

import scala.util.{Try, Success, Failure}


private[rabbit] object RPCProcessor {
  def apply(processor: RabbitRPCProcessor, channel: Channel): RPCProcessor =
    new RPCProcessor(processor, channel) with AMQPRabbitFunctions

  def props(processor: RabbitRPCProcessor, channel: Channel): Props =
    Props(RPCProcessor(processor, channel))
}

private[rabbit] class RPCProcessor(processor: RabbitRPCProcessor, channel: Channel) extends Actor
                                                                           with ActorLogging {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.protocol._

  override def receive: Actor.Receive = {
    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      Try(processor.process(hd)) match {
        case Success(result) =>
          publishResponse(channel, result, properties)
          basicAck(channel)(envelope.getDeliveryTag)
        case Failure(cause) if !envelope.isRedeliver =>
          log.error(cause, s"processing $hd failed, requeueing.")
          basicReject(channel)(envelope.getDeliveryTag, requeue = true)
        case Failure(cause) if envelope.isRedeliver =>
          log.error(cause, s"processing $hd failed twice, acking.")
          val recoverResult = processor.recover(hd, cause)
          publishResponse(channel, recoverResult, properties)
          basicReject(channel)(envelope.getDeliveryTag, requeue = false)
      }
      context.stop(self)
  }

  private def publishResponse(channel: Channel, result: RabbitRPCResult, properties: AMQP.BasicProperties): Unit = {
    result match {
      case RabbitRPCResult(Some(data), resultProperties) =>
        val props = resultProperties.getOrElse(new AMQP.BasicProperties()).builder().correlationId(properties.getCorrelationId).build()
        basicPublish(channel)("", properties.getReplyTo, data, mandatory = true, immediate = false, Some(props))
      case _ => ()
    }
  }

}
