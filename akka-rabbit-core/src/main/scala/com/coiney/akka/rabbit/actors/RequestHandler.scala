package com.coiney.akka.rabbit.actors

import scala.util.{Failure, Success, Try}


trait RequestHandler {
  import com.coiney.akka.rabbit.protocol
  import com.coiney.akka.rabbit.protocol.{RabbitRequest, RabbitResponse}

  protected def handleRequest[T](request: RabbitRequest)(f: () => T): RabbitResponse = {
    Try(f()) match {
      case Success(())       => protocol.Success(request, None)
      case Success(response) => protocol.Success(request, Some(response))
      case Failure(cause)    => protocol.Failure(request, cause)
    }
  }

}
