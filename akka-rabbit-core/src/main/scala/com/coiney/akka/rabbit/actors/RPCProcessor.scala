package com.coiney.akka.rabbit.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.rabbitmq.client.{AMQP, Channel}

import scala.util.{Try, Success, Failure}

import com.coiney.akka.rabbit.RPC._


private[rabbit] object RPCProcessor {
  def apply(processor: Processor, channel: Channel): RPCProcessor = new RPCProcessor(processor, channel)

  def props(processor: Processor, channel: Channel): Props = Props(RPCProcessor(processor, channel))
}

private[rabbit] class RPCProcessor(processor: Processor, channel: Channel) extends Actor
                                                                           with ActorLogging {
  import com.coiney.akka.rabbit.messages._

  override def receive: Actor.Receive = {
    case hd @ HandleDelivery(consumerTag, envelope, properties, body) =>
      Try(processor.process(hd)) match {
        case Success(result) =>
          publishResponse(channel, result, properties)
          channel.basicAck(envelope.getDeliveryTag, false)
        case Failure(cause) if !envelope.isRedeliver =>
          log.error(cause, s"processing $hd failed, requeueing.")
          channel.basicReject(envelope.getDeliveryTag, false)
        case Failure(cause) if envelope.isRedeliver =>
          log.error(cause, s"processing $hd failed twice, acking.")
          val recoverResult = processor.recover(hd, cause)
          publishResponse(channel, recoverResult, properties)
          channel.basicReject(envelope.getDeliveryTag, true)
      }
      context.stop(self)
  }

  private def publishResponse(channel: Channel, result: Result, properties: AMQP.BasicProperties): Unit = {
    result match {
      case Result(Some(data), resultProperties) =>
        val props = resultProperties.getOrElse(new AMQP.BasicProperties()).builder().correlationId(properties.getCorrelationId).build()
        channel.basicPublish("", properties.getReplyTo, true, false, props, data)
      case _ => ()
    }
  }


}
