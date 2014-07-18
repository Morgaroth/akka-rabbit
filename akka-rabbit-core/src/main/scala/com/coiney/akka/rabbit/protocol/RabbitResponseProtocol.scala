package com.coiney.akka.rabbit.protocol


sealed trait RabbitResponse
case class OK(request: RabbitRequest, result: Option[Any] = None) extends RabbitResponse
case class ERROR(request: RabbitRequest, cause: Throwable) extends RabbitResponse
case class DisconnectedError(request: RabbitRequest) extends RabbitResponse
