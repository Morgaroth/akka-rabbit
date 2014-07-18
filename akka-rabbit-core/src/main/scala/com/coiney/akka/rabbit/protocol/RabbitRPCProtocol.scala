package com.coiney.akka.rabbit.protocol

import com.rabbitmq.client.AMQP


case class RabbitRPCResult(data: Option[Array[Byte]], properties: Option[AMQP.BasicProperties] = None)

trait RabbitRPCProcessor {
  def process(hd: HandleDelivery): RabbitRPCResult

  def recover(hd: HandleDelivery, cause: Throwable): RabbitRPCResult
}

case class RabbitRPCRequest(publishes: List[Publish], numberOfResponses: Int = 1)

case class RabbitRPCResponse(handleDeliveries: List[HandleDelivery])