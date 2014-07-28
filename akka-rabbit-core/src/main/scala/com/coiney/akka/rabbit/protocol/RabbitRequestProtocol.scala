package com.coiney.akka.rabbit.protocol

import akka.actor.ActorRef
import com.rabbitmq.client.AMQP

import com.coiney.akka.rabbit.{ExchangeConfig, QueueConfig}

import scala.concurrent.duration.FiniteDuration


sealed trait RabbitRequest
case class AddConfirmListener(listener: ActorRef) extends RabbitRequest
case class AddReturnListener(listener: ActorRef) extends RabbitRequest
case class AddShutdownListener(listener: ActorRef) extends RabbitRequest

case class DeclareQueue(queueConfig: QueueConfig) extends RabbitRequest
case class DeclareQueuePassive(name: String) extends RabbitRequest
case class DeleteQueue(name: String, ifUnused: Boolean = false, ifEmpty: Boolean = false) extends RabbitRequest
case class PurgeQueue(name: String) extends RabbitRequest
case class BindQueue(name: String, exchange: String, routingKey: String, arguments: Map[String, AnyRef] = Map.empty) extends RabbitRequest
case class UnbindQueue(name: String, exchange: String, routingKey: String) extends RabbitRequest

case class DeclareExchange(exchangeConfig: ExchangeConfig) extends RabbitRequest
case class DeclareExchangePassive(name: String) extends RabbitRequest
case class DeleteExchange(name: String) extends RabbitRequest
case class BindExchange(destination: String, source: String, routingKey: String, arguments: Map[String, AnyRef] = Map.empty) extends RabbitRequest
case class UnbindExchange(destination: String, source: String, routingKey: String) extends RabbitRequest

case class Publish(exchange: String, routingKey: String, body: Array[Byte], mandatory: Boolean = true, immediate: Boolean = false, properties: Option[AMQP.BasicProperties] = None) extends RabbitRequest
case class Transaction(pubs: Seq[Publish]) extends RabbitRequest
case class Ack(deliveryTag: Long) extends RabbitRequest
case class Reject(deliveryTag: Long, requeue: Boolean = true) extends RabbitRequest

case class Get(queue: String, autoAck: Boolean = false) extends RabbitRequest

case object ConfirmSelect extends RabbitRequest
case class WaitForConfirms(timeout: Option[FiniteDuration]) extends RabbitRequest
case class WaitForConfirmsOrDie(timeout: Option[FiniteDuration]) extends RabbitRequest

case class AddConsumer(listener: ActorRef, consumer: ActorRef) extends RabbitRequest

case class ConsumeQueue(queueConfig: QueueConfig) extends RabbitRequest
case class CancelConsume(consumerTag: String) extends RabbitRequest