package com.coiney.akka.rabbit.actors

import akka.actor.ActorRef
import com.rabbitmq.client.AMQP.{Queue, Exchange, Tx}
import com.rabbitmq.client._

import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration

trait RabbitFunctions {
  import com.coiney.akka.rabbit.messages._

  def addConfirmListener(listener: ActorRef)(channel: Channel): Unit
  def addReturnListener(listener: ActorRef)(channel: Channel): Unit
  def addShutdownListener(listener: ActorRef)(channel: Channel): Unit
  def queueDeclare(name: String, durable: Boolean, exclusive: Boolean, autoDelete: Boolean, arguments: Map[String, AnyRef])(channel: Channel): Queue.DeclareOk
  def queueDeclarePassive(name: String)(channel: Channel): Queue.DeclareOk
  def queueDelete(name: String, ifUnused: Boolean, ifEmpty: Boolean)(channel: Channel): Queue.DeleteOk
  def queuePurge(name: String)(channel: Channel): Queue.PurgeOk
  def queueBind(name: String, exchange: String, routingKey: String, arguments: Map[String, AnyRef])(channel: Channel): Queue.BindOk
  def queueUnbind(name: String, exchange: String, routingKey: String)(channel: Channel): Queue.UnbindOk
  def exchangeDeclare(name: String, exchangeType: String, durable: Boolean, autoDelete: Boolean, arguments: Map[String, AnyRef])(channel: Channel): Exchange.DeclareOk
  def exchangeDeclarePassive(name: String)(channel: Channel): Exchange.DeclareOk
  def exchangeDelete(name: String)(channel: Channel): Exchange.DeleteOk
  def exchangeBind(destination: String, source: String, routingKey: String, arguments: Map[String, AnyRef])(channel: Channel): Exchange.BindOk
  def exchangeUnbind(destination: String, source: String, routingKey: String)(channel: Channel): Exchange.UnbindOk
  def basicPublish(exchange: String, routingKey: String, body: Array[Byte], mandatory: Boolean, immediate: Boolean, properties: Option[AMQP.BasicProperties])(channel: Channel): Unit
  def commitTransaction(publishes: Seq[Publish])(channel: Channel): Tx.CommitOk
  def basicAck(deliveryTag: Long)(channel: Channel): Unit
  def basicReject(deliveryTag: Long, requeue: Boolean)(channel: Channel): Unit
  def basicGet(queue: String, autoAck: Boolean)(channel: Channel): Unit
  def confirmSelect(channel: Channel): Unit
  def waitForConfirms(timeout: Option[FiniteDuration])(channel: Channel): Boolean
  def waitForConfirmsOrDie(timeout: Option[FiniteDuration])(channel: Channel): Unit
  def addConsumer(listener: ActorRef)(channel: Channel): DefaultConsumer

  def close(channel: Channel): Unit
}


trait AMQPRabbitFunctions extends RabbitFunctions {
  import com.coiney.akka.rabbit.messages._

  def addConfirmListener(listener: ActorRef)(channel: Channel): Unit = {
    channel.addConfirmListener(new ConfirmListener {
      override def handleAck(deliveryTag: Long, multiple: Boolean): Unit =
        listener ! HandleAck(deliveryTag, multiple)

      override def handleNack(deliveryTag: Long, multiple: Boolean): Unit =
        listener ! HandleNack(deliveryTag, multiple)
    })
  }

  def addReturnListener(listener: ActorRef)(channel: Channel): Unit = {
    channel.addReturnListener(new ReturnListener {
      override def handleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        listener ! HandleReturn(replyCode, replyText, exchange, routingKey, properties, body)
    })
  }

  def addShutdownListener(listener: ActorRef)(channel: Channel): Unit = {
    channel.addShutdownListener(new ShutdownListener {
      override def shutdownCompleted(cause: ShutdownSignalException): Unit =
        listener ! HandleShutdown(cause)
    })
  }

  def queueDeclare(name: String, durable: Boolean, exclusive: Boolean, autoDelete: Boolean, arguments: Map[String, AnyRef])(channel: Channel): Queue.DeclareOk = {
    channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)
  }

  def queueDeclarePassive(name: String)(channel: Channel): Queue.DeclareOk = {
    channel.queueDeclarePassive(name)
  }

  def queueDelete(name: String, ifUnused: Boolean, ifEmpty: Boolean)(channel: Channel): Queue.DeleteOk = {
    channel.queueDelete(name, ifUnused, ifEmpty)
  }

  def queuePurge(name: String)(channel: Channel): Queue.PurgeOk = {
    channel.queuePurge(name)
  }

  def queueBind(name: String, exchange: String, routingKey: String, arguments: Map[String, AnyRef])(channel: Channel): Queue.BindOk = {
    channel.queueBind(name, exchange, routingKey, arguments)
  }

  def queueUnbind(name: String, exchange: String, routingKey: String)(channel: Channel): Queue.UnbindOk = {
    channel.queueUnbind(name, exchange, routingKey)
  }

  def exchangeDeclare(name: String, exchangeType: String, durable: Boolean, autoDelete: Boolean, arguments: Map[String, AnyRef])(channel: Channel): Exchange.DeclareOk = {
    channel.exchangeDeclare(name, exchangeType, durable, autoDelete, arguments)
  }

  def exchangeDeclarePassive(name: String)(channel: Channel): Exchange.DeclareOk = {
    channel.exchangeDeclarePassive(name)
  }

  def exchangeDelete(name: String)(channel: Channel): Exchange.DeleteOk = {
    channel.exchangeDelete(name)
  }

  def exchangeBind(destination: String, source: String, routingKey: String, arguments: Map[String, AnyRef])(channel: Channel): Exchange.BindOk = {
    channel.exchangeBind(destination, source, routingKey, arguments)
  }

  def exchangeUnbind(destination: String, source: String, routingKey: String)(channel: Channel): Exchange.UnbindOk = {
    channel.exchangeUnbind(destination, source, routingKey)
  }

  def basicPublish(exchange: String, routingKey: String, body: Array[Byte], mandatory: Boolean, immediate: Boolean, properties: Option[AMQP.BasicProperties])(channel: Channel): Unit = {
    val props = properties.getOrElse(new AMQP.BasicProperties.Builder().build())
    channel.basicPublish(exchange, routingKey, mandatory, immediate, props, body)
  }

  def commitTransaction(publishes: Seq[Publish])(channel: Channel): Tx.CommitOk = {
    channel.txSelect()
    publishes.foreach{ p =>
      val props = p.properties.getOrElse(new AMQP.BasicProperties.Builder().build())
      channel.basicPublish(p.exchange, p.routingKey, p.mandatory, p.immediate, props, p.body)
    }
    channel.txCommit()
  }

  def basicAck(deliveryTag: Long)(channel: Channel): Unit = {
    channel.basicAck(deliveryTag, false)
  }

  def basicReject(deliveryTag: Long, requeue: Boolean)(channel: Channel): Unit = {
    channel.basicReject(deliveryTag, false)
  }

  def basicGet(queue: String, autoAck: Boolean)(channel: Channel): Unit = {
    channel.basicGet(queue, autoAck)
  }

  def confirmSelect(channel: Channel): Unit = {
    channel.confirmSelect()
  }

  def waitForConfirms(timeout: Option[FiniteDuration])(channel: Channel): Boolean = {
    timeout match {
      case None    => channel.waitForConfirms()
      case Some(t) => channel.waitForConfirms(t.toMillis)
    }
  }

  def waitForConfirmsOrDie(timeout: Option[FiniteDuration])(channel: Channel): Unit = {
    timeout match {
      case None    => channel.waitForConfirmsOrDie()
      case Some(t) => channel.waitForConfirmsOrDie(t.toMillis)
    }
  }

  def addConsumer(listener: ActorRef)(channel: Channel): DefaultConsumer = {
    new DefaultConsumer(channel){
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        listener ! HandleDelivery(consumerTag, envelope, properties, body)

      override def handleCancel(consumerTag: String): Unit =
        listener ! HandleCancel(consumerTag)
    }
  }

  def close(channel: Channel): Unit = {
    channel.close()
  }

}

