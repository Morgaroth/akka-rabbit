package com.coiney.akka.rabbit.protocol


sealed trait RabbitResponse
case class Success(request: RabbitRequest, result: Option[Any] = None) extends RabbitResponse
case class Failure(request: RabbitRequest, cause: Throwable) extends RabbitResponse
case class DisconnectedError(request: RabbitRequest) extends RabbitResponse
