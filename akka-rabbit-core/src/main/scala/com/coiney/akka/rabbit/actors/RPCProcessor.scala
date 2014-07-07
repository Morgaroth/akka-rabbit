package com.coiney.akka.rabbit.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.rabbitmq.client.{AMQP, Channel}

import scala.util.{Try, Success, Failure}

import com.coiney.akka.rabbit.RPC._


private[rabbit] object RPCProcessor {
  def apply(processor: Processor, channel: Channel): RPCProcessor =
    new RPCProcessor(processor, channel) with AMQPRabbitFunctions

  def props(processor: Processor, channel: Channel): Props =
    Props(RPCProcessor(processor, channel))
}

private[rabbit] class RPCProcessor(processor: Processor, channel: Channel) extends Actor
                                                                           with ActorLogging {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.messages._

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

  private def publishResponse(channel: Channel, result: Result, properties: AMQP.BasicProperties): Unit = {
    result match {
      case Result(Some(data), resultProperties) =>
        val props = resultProperties.getOrElse(new AMQP.BasicProperties()).builder().correlationId(properties.getCorrelationId).build()
        basicPublish(channel)("", properties.getReplyTo, data, mandatory = true, immediate = false, Some(props))
      case _ => ()
    }
  }

}
