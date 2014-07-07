package com.coiney.akka.rabbit

import com.rabbitmq.client.AMQP

import com.coiney.akka.rabbit.messages.{HandleDelivery, Publish}


package object RPC {

  case class Result(data: Option[Array[Byte]], properties: Option[AMQP.BasicProperties] = None)

  trait Processor {
    def process(hd: HandleDelivery): Result

    def recover(hd: HandleDelivery, cause: Throwable): Result
  }

  case class Request(publishes: List[Publish], numberOfResponses: Int = 1)

  case class Response(handleDeliveries: List[HandleDelivery])

}
