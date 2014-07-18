package com.coiney.akka.rabbit.protocol

import com.rabbitmq.client.{Envelope, ShutdownSignalException, AMQP}


case class HandleAck(deliveryTag: Long, multiple: Boolean)
case class HandleNack(deliveryTag: Long, multiple: Boolean)

case class HandleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: AMQP.BasicProperties, body: Array[Byte])

case class HandleShutdown(cause: ShutdownSignalException)

case class HandleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte])
case class HandleCancel(consumerTag: String)