package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client._

import scala.util.{Failure, Success, Try}


private[rabbit] object ChannelHandler {
  def apply(channel: Channel): ChannelHandler = new ChannelHandler(channel) with AMQPRabbitFunctions

  def props(channel: Channel): Props = Props(ChannelHandler(channel))
}


private[rabbit] class ChannelHandler(channel: Channel) extends Actor
                                                       with ActorLogging {
  this: RabbitFunctions =>
  import com.coiney.akka.rabbit.messages._

  override def postStop(): Unit = Try {
    closeChannel(channel)
  }

  override def receive: Actor.Receive = {

    case req @ AddConfirmListener(listener) =>
      sender ! handleRequest(req){ () =>
        addConfirmListener(channel)(listener)
      }

    case req @ AddReturnListener(listener) =>
      sender ! handleRequest(req){ () =>
        addReturnListener(channel)(listener)
      }

    case req @ AddShutdownListener(listener) =>
      sender ! handleRequest(req){ () =>
        addShutdownListener(channel)(listener)
      }

    case req @ DeclareQueue(name, durable, exclusive, autoDelete, arguments) =>
      sender ! handleRequest(req){ () =>
        queueDeclare(channel)(name, durable, exclusive, autoDelete, arguments)
      }

    case req @ DeclareQueuePassive(name) =>
      sender ! handleRequest(req){ () =>
        queueDeclarePassive(channel)(name)
      }

    case req @ DeleteQueue(name, ifUnused, ifEmpty) =>
      sender ! handleRequest(req){ () =>
        queueDelete(channel)(name, ifUnused, ifEmpty)
      }

    case req @ PurgeQueue(name) =>
      sender ! handleRequest(req){ () =>
        queuePurge(channel)(name)
      }

    case req @ BindQueue(name, exchange, routingKey, arguments) =>
      sender ! handleRequest(req){ () =>
        queueBind(channel)(name, exchange, routingKey, arguments)
      }

    case req @ UnbindQueue(name, exchange, routingKey) =>
      sender ! handleRequest(req){ () =>
        queueUnbind(channel)(name, exchange, routingKey)
      }

    case req @ DeclareExchange(name, exchangeType, durable, autoDelete, arguments) =>
      sender ! handleRequest(req){ () =>
        exchangeDeclare(channel)(name, exchangeType, durable, autoDelete, arguments)
      }

    case req @ DeclareExchangePassive(name) =>
      sender ! handleRequest(req){ () =>
        exchangeDeclarePassive(channel)(name)
      }

    case req @ DeleteExchange(name) =>
      sender ! handleRequest(req){ () =>
        exchangeDelete(channel)(name)
      }

    case req @ BindExchange(destination, source, routingKey, arguments) =>
      sender ! handleRequest(req){ () =>
        exchangeBind(channel)(destination, source, routingKey, arguments)
      }

    case req @ UnbindExchange(destination, source, routingKey) =>
      sender ! handleRequest(req){ () =>
        exchangeUnbind(channel)(destination, source, routingKey)
      }

    case req @ Publish(exchange, routingKey, body, mandatory, immediate, properties) =>
      log.debug(s"Publishing $req")
      sender ! handleRequest(req){ () =>
        basicPublish(channel)(exchange, routingKey, body,  mandatory, immediate, properties)
      }

    case req @ Transaction(pubs) =>
      sender ! handleRequest(req){ () =>
        commitTransaction(channel)(pubs)
      }

    case req @ Ack(deliveryTag) =>
      sender ! handleRequest(req){ () =>
        basicAck(channel)(deliveryTag)
      }

    case req @ Reject(deliveryTag, requeue) =>
      sender ! handleRequest(req){ () =>
        basicReject(channel)(deliveryTag, requeue)
      }

    case req @ Get(queue, autoAck) =>
      sender ! handleRequest(req){ () =>
        basicGet(channel)(queue, autoAck)
      }

    case req @ ConfirmSelect =>
      sender ! handleRequest(req){ () =>
        confirmSelect(channel)
      }

    case req @ WaitForConfirms(timeout) =>
      sender ! handleRequest(req){ () =>
        waitForConfirms(channel)(timeout)
      }

    case req @ WaitForConfirmsOrDie(timeout) =>
      sender ! handleRequest(req){ () =>
        waitForConfirmsOrDie(channel)(timeout)
      }

    case req @ AddConsumer(listener) =>
      sender ! handleRequest(req){ () =>
        addConsumer(channel)(listener)
      }

  }

  private def handleRequest[T](request: Request)(f: () => T): Response = {
    Try(f()) match {
      case Success(())       => OK(request, None)
      case Success(response) => OK(request, Some(response))
      case Failure(cause)    => ERROR(request, cause)
    }
  }

}
