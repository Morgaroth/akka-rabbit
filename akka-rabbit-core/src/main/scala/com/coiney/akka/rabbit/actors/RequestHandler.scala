package com.coiney.akka.rabbit.actors

import scala.util.{Failure, Success, Try}


trait RequestHandler {
  import com.coiney.akka.rabbit.protocol._

  protected def handleRequest[T](request: RabbitRequest)(f: () => T): RabbitResponse = {
    Try(f()) match {
      case Success(())       => OK(request, None)
      case Success(response) => OK(request, Some(response))
      case Failure(cause)    => ERROR(request, cause)
    }
  }

}
