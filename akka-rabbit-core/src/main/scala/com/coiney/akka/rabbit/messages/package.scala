package com.coiney.akka.rabbit

import akka.actor.ActorRef
import com.rabbitmq.client.{ShutdownSignalException, AMQP, Envelope}

import scala.concurrent.duration.FiniteDuration


package object messages {

  sealed trait Request
  case class AddConfirmListener(listener: ActorRef) extends Request
  case class AddReturnListener(listener: ActorRef) extends Request
  case class AddShutdownListener(listener: ActorRef) extends Request

  case class DeclareQueue(queueConfig: QueueConfig) extends Request
  case class DeclareQueuePassive(name: String) extends Request
  case class DeleteQueue(name: String, ifUnused: Boolean = false, ifEmpty: Boolean = false) extends Request
  case class PurgeQueue(name: String) extends Request
  case class BindQueue(name: String, exchange: String, routingKey: String, arguments: Map[String, AnyRef] = Map.empty) extends Request
  case class UnbindQueue(name: String, exchange: String, routingKey: String) extends Request

  case class DeclareExchange(exchangeConfig: ExchangeConfig) extends Request
  case class DeclareExchangePassive(name: String) extends Request
  case class DeleteExchange(name: String) extends Request
  case class BindExchange(destination: String, source: String, routingKey: String, arguments: Map[String, AnyRef] = Map.empty) extends Request
  case class UnbindExchange(destination: String, source: String, routingKey: String) extends Request

  case class Publish(exchange: String, routingKey: String, body: Array[Byte], mandatory: Boolean = true, immediate: Boolean = false, properties: Option[AMQP.BasicProperties] = None) extends Request
  case class Transaction(pubs: Seq[Publish]) extends Request
  case class Ack(deliveryTag: Long) extends Request
  case class Reject(deliveryTag: Long, requeue: Boolean = true) extends Request

  case class Get(queue: String, autoAck: Boolean = false) extends Request

  case object ConfirmSelect extends Request
  case class WaitForConfirms(timeout: Option[FiniteDuration]) extends Request
  case class WaitForConfirmsOrDie(timeout: Option[FiniteDuration]) extends Request

  case class AddConsumer(listener: ActorRef, consumer: ActorRef) extends Request

  case class ConsumeQueue(queueConfig: QueueConfig) extends Request
  case class CancelConsume(consumerTag: String) extends Request

  sealed trait Response
  case class OK(request: Request, result: Option[Any] = None) extends Response
  case class ERROR(request: Request, cause: Throwable) extends Response
  case class DisconnectedError(request: Request) extends Response


  case class HandleAck(deliveryTag: Long, multiple: Boolean)
  case class HandleNack(deliveryTag: Long, multiple: Boolean)

  case class HandleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: AMQP.BasicProperties, body: Array[Byte])

  case class HandleShutdown(cause: ShutdownSignalException)

  case class HandleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte])
  case class HandleCancel(consumerTag: String)

}
