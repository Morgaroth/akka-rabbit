package com.coiney.akka.rabbit.actors

import scala.util.{Failure, Success, Try}


trait RequestHandler {
  import com.coiney.akka.rabbit.messages._

  protected def handleRequest[T](request: Request)(f: () => T): Response = {
    Try(f()) match {
      case Success(())       => OK(request, None)
      case Success(response) => OK(request, Some(response))
      case Failure(cause)    => ERROR(request, cause)
    }
  }

}
