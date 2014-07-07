package com.coiney.akka.rabbit.actors

import akka.actor.ActorRef
import com.rabbitmq.client.AMQP.{Queue, Exchange, Tx}
import com.rabbitmq.client._

import com.coiney.akka.rabbit.{ExchangeConfig, QueueConfig, ChannelConfig}

import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration


trait RabbitFunctions {
  import com.coiney.akka.rabbit.messages._

  // connection methods
  def addShutdownListener(connection: Connection)(listener: ActorRef): Unit
  def createChannel(connection: Connection): Channel
  def closeConnection(connection: Connection): Unit

  // channel methods
  def addConfirmListener(channel: Channel)(listener: ActorRef): Unit
  def addReturnListener(channel: Channel)(listener: ActorRef): Unit
  def addShutdownListener(channel: Channel)(listener: ActorRef): Unit
  def queueDeclare(channel: Channel)(queueConfig: QueueConfig): Queue.DeclareOk
  def queueDeclarePassive(channel: Channel)(name: String): Queue.DeclareOk
  def queueDelete(channel: Channel)(name: String, ifUnused: Boolean, ifEmpty: Boolean): Queue.DeleteOk
  def queuePurge(channel: Channel)(name: String): Queue.PurgeOk
  def queueBind(channel: Channel)(name: String, exchange: String, routingKey: String, arguments: Map[String, AnyRef]): Queue.BindOk
  def queueUnbind(channel: Channel)(name: String, exchange: String, routingKey: String): Queue.UnbindOk
  def queueConsume(channel: Channel)(queueConfig: QueueConfig, autoAck: Boolean, consumer: DefaultConsumer): String
  def exchangeDeclare(channel: Channel)(exchangeConfig: ExchangeConfig): Exchange.DeclareOk
  def exchangeDeclarePassive(channel: Channel)(name: String): Exchange.DeclareOk
  def exchangeDelete(channel: Channel)(name: String): Exchange.DeleteOk
  def exchangeBind(channel: Channel)(destination: String, source: String, routingKey: String, arguments: Map[String, AnyRef]): Exchange.BindOk
  def exchangeUnbind(channel: Channel)(destination: String, source: String, routingKey: String): Exchange.UnbindOk
  def basicPublish(channel: Channel)(exchange: String, routingKey: String, body: Array[Byte], mandatory: Boolean, immediate: Boolean, properties: Option[AMQP.BasicProperties]): Unit
  def commitTransaction(channel: Channel)(publishes: Seq[Publish]): Tx.CommitOk
  def basicConsume(channel: Channel)(queue: String, autoAck: Boolean, consumer: DefaultConsumer): String
  def basicCancel(channel: Channel)(consumerTag: String): Unit
  def basicAck(channel: Channel)(deliveryTag: Long): Unit
  def basicReject(channel: Channel)(deliveryTag: Long, requeue: Boolean): Unit
  def basicGet(channel: Channel)(queue: String, autoAck: Boolean): Unit
  def basicQoS(channel: Channel)(prefetchCount: Int, global: Boolean): Unit
  def confirmSelect(channel: Channel): Unit
  def waitForConfirms(channel: Channel)(timeout: Option[FiniteDuration]): Boolean
  def waitForConfirmsOrDie(channel: Channel)(timeout: Option[FiniteDuration]): Unit
  def addConsumer(channel: Channel)(listener: ActorRef): DefaultConsumer
  def closeChannel(channel: Channel): Unit

  def configureChannel(channel: Channel)(channelConfig: ChannelConfig): Unit
}


trait AMQPRabbitFunctions extends RabbitFunctions {
  import com.coiney.akka.rabbit.messages._

  def addShutdownListener(connection: Connection)(listener: ActorRef): Unit = {
    connection.addShutdownListener(new ShutdownListener {
      override def shutdownCompleted(cause: ShutdownSignalException): Unit =
        listener ! HandleShutdown(cause)
    })
  }

  def createChannel(connection: Connection): Channel = {
    connection.createChannel()
  }

  def closeConnection(connection: Connection): Unit = {
    connection.close()
  }

  def addConfirmListener(channel: Channel)(listener: ActorRef): Unit = {
    channel.addConfirmListener(new ConfirmListener {
      override def handleAck(deliveryTag: Long, multiple: Boolean): Unit =
        listener ! HandleAck(deliveryTag, multiple)

      override def handleNack(deliveryTag: Long, multiple: Boolean): Unit =
        listener ! HandleNack(deliveryTag, multiple)
    })
  }

  def addReturnListener(channel: Channel)(listener: ActorRef): Unit = {
    channel.addReturnListener(new ReturnListener {
      override def handleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        listener ! HandleReturn(replyCode, replyText, exchange, routingKey, properties, body)
    })
  }

  def addShutdownListener(channel: Channel)(listener: ActorRef): Unit = {
    channel.addShutdownListener(new ShutdownListener {
      override def shutdownCompleted(cause: ShutdownSignalException): Unit =
        listener ! HandleShutdown(cause)
    })
  }

  def queueDeclare(channel: Channel)(queueConfig: QueueConfig): Queue.DeclareOk = {
    channel.queueDeclare(queueConfig.name, queueConfig.durable, queueConfig.exclusive, queueConfig.autoDelete, queueConfig.arguments)
  }

  def queueDeclarePassive(channel: Channel)(name: String): Queue.DeclareOk = {
    channel.queueDeclarePassive(name)
  }

  def queueDelete(channel: Channel)(name: String, ifUnused: Boolean, ifEmpty: Boolean): Queue.DeleteOk = {
    channel.queueDelete(name, ifUnused, ifEmpty)
  }

  def queuePurge(channel: Channel)(name: String): Queue.PurgeOk = {
    channel.queuePurge(name)
  }

  def queueBind(channel: Channel)(name: String, exchange: String, routingKey: String, arguments: Map[String, AnyRef]): Queue.BindOk = {
    channel.queueBind(name, exchange, routingKey, arguments)
  }

  def queueUnbind(channel: Channel)(name: String, exchange: String, routingKey: String): Queue.UnbindOk = {
    channel.queueUnbind(name, exchange, routingKey)
  }

  def queueConsume(channel: Channel)(queueConfig: QueueConfig, autoAck: Boolean, consumer: DefaultConsumer): String = {
    queueDeclare(channel)(queueConfig)
    val consumerTag = basicConsume(channel)(queueConfig.name, autoAck, consumer)
    consumerTag
  }

  def exchangeDeclare(channel: Channel)(exchangeConfig: ExchangeConfig): Exchange.DeclareOk = {
    channel.exchangeDeclare(exchangeConfig.name, exchangeConfig.exchangeType, exchangeConfig.durable, exchangeConfig.autoDelete, exchangeConfig.arguments)
  }

  def exchangeDeclarePassive(channel: Channel)(name: String): Exchange.DeclareOk = {
    channel.exchangeDeclarePassive(name)
  }

  def exchangeDelete(channel: Channel)(name: String): Exchange.DeleteOk = {
    channel.exchangeDelete(name)
  }

  def exchangeBind(channel: Channel)(destination: String, source: String, routingKey: String, arguments: Map[String, AnyRef]): Exchange.BindOk = {
    channel.exchangeBind(destination, source, routingKey, arguments)
  }

  def exchangeUnbind(channel: Channel)(destination: String, source: String, routingKey: String): Exchange.UnbindOk = {
    channel.exchangeUnbind(destination, source, routingKey)
  }

  def basicPublish(channel: Channel)(exchange: String, routingKey: String, body: Array[Byte], mandatory: Boolean, immediate: Boolean, properties: Option[AMQP.BasicProperties]): Unit = {
    val props = properties.getOrElse(new AMQP.BasicProperties.Builder().build())
    channel.basicPublish(exchange, routingKey, mandatory, immediate, props, body)
  }

  def commitTransaction(channel: Channel)(publishes: Seq[Publish]): Tx.CommitOk = {
    channel.txSelect()
    publishes.foreach{ p =>
      val props = p.properties.getOrElse(new AMQP.BasicProperties.Builder().build())
      channel.basicPublish(p.exchange, p.routingKey, p.mandatory, p.immediate, props, p.body)
    }
    channel.txCommit()
  }

  def basicConsume(channel: Channel)(queue: String, autoAck: Boolean, consumer: DefaultConsumer): String = {
    channel.basicConsume(queue, autoAck, consumer)
  }

  def basicCancel(channel: Channel)(consumerTag: String): Unit = {
    channel.basicCancel(consumerTag)
  }

  def basicAck(channel: Channel)(deliveryTag: Long): Unit = {
    channel.basicAck(deliveryTag, false)
  }

  def basicReject(channel: Channel)(deliveryTag: Long, requeue: Boolean): Unit = {
    channel.basicReject(deliveryTag, false)
  }

  def basicGet(channel: Channel)(queue: String, autoAck: Boolean): Unit = {
    channel.basicGet(queue, autoAck)
  }

  def basicQoS(channel: Channel)(prefetchCount: Int, global: Boolean): Unit = {
    channel.basicQos(prefetchCount, global)
  }

  def confirmSelect(channel: Channel): Unit = {
    channel.confirmSelect()
  }

  def waitForConfirms(channel: Channel)(timeout: Option[FiniteDuration]): Boolean = {
    timeout match {
      case None    => channel.waitForConfirms()
      case Some(t) => channel.waitForConfirms(t.toMillis)
    }
  }

  def waitForConfirmsOrDie(channel: Channel)(timeout: Option[FiniteDuration]): Unit = {
    timeout match {
      case None    => channel.waitForConfirmsOrDie()
      case Some(t) => channel.waitForConfirmsOrDie(t.toMillis)
    }
  }

  def addConsumer(channel: Channel)(listener: ActorRef): DefaultConsumer = {
    new DefaultConsumer(channel){
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
        listener ! HandleDelivery(consumerTag, envelope, properties, body)

      override def handleCancel(consumerTag: String): Unit =
        listener ! HandleCancel(consumerTag)
    }
  }

  def closeChannel(channel: Channel): Unit = {
    channel.close()
  }

  def configureChannel(channel: Channel)(channelConfig: ChannelConfig): Unit = {
    basicQoS(channel)(channelConfig.channelPrefetchCount, global = true)
    basicQoS(channel)(channelConfig.consumerPrefetchCount, global = false)
  }

}

