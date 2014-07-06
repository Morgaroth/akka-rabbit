package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client._

import scala.collection.JavaConversions._
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
        addConfirmListener(listener)(channel)
      }

    case req @ AddReturnListener(listener) =>
      sender ! handleRequest(req){ () =>
        addReturnListener(listener)(channel)
      }

    case req @ AddShutdownListener(listener) =>
      sender ! handleRequest(req){ () =>
        addShutdownListener(listener)(channel)
      }

    case req @ DeclareQueue(name, durable, exclusive, autoDelete, arguments) =>
      sender ! handleRequest(req){ () =>
        queueDeclare(name, durable, exclusive, autoDelete, arguments)(channel)
      }

    case req @ DeclareQueuePassive(name) =>
      sender ! handleRequest(req){ () =>
        queueDeclarePassive(name)(channel)
      }

    case req @ DeleteQueue(name, ifUnused, ifEmpty) =>
      sender ! handleRequest(req){ () =>
        queueDelete(name, ifUnused, ifEmpty)(channel)
      }

    case req @ PurgeQueue(name) =>
      sender ! handleRequest(req){ () =>
        queuePurge(name)(channel)
      }

    case req @ BindQueue(name, exchange, routingKey, arguments) =>
      sender ! handleRequest(req){ () =>
        queueBind(name, exchange, routingKey, arguments)(channel)
      }

    case req @ UnbindQueue(name, exchange, routingKey) =>
      sender ! handleRequest(req){ () =>
        queueUnbind(name, exchange, routingKey)(channel)
      }

    case req @ DeclareExchange(name, exchangeType, durable, autoDelete, arguments) =>
      sender ! handleRequest(req){ () =>
        exchangeDeclare(name, exchangeType, durable, autoDelete, arguments)(channel)
      }

    case req @ DeclareExchangePassive(name) =>
      sender ! handleRequest(req){ () =>
        exchangeDeclarePassive(name)(channel)
      }

    case req @ DeleteExchange(name) =>
      sender ! handleRequest(req){ () =>
        exchangeDelete(name)(channel)
      }

    case req @ BindExchange(destination, source, routingKey, arguments) =>
      sender ! handleRequest(req){ () =>
        exchangeBind(destination, source, routingKey, arguments)(channel)
      }

    case req @ UnbindExchange(destination, source, routingKey) =>
      sender ! handleRequest(req){ () =>
        exchangeUnbind(destination, source, routingKey)(channel)
      }

    case req @ Publish(exchange, routingKey, body, mandatory, immediate, properties) =>
      log.debug(s"Publishing $req")
      sender ! handleRequest(req){ () =>
        basicPublish(exchange, routingKey, body,  mandatory, immediate, properties)(channel)
      }

    case req @ Transaction(pubs) =>
      sender ! handleRequest(req){ () =>
        commitTransaction(pubs)(channel)
      }

    case req @ Ack(deliveryTag) =>
      sender ! handleRequest(req){ () =>
        basicAck(deliveryTag)(channel)
      }

    case req @ Reject(deliveryTag, requeue) =>
      sender ! handleRequest(req){ () =>
        basicReject(deliveryTag, requeue)(channel)
      }

    case req @ Get(queue, autoAck) =>
      sender ! handleRequest(req){ () =>
        basicGet(queue, autoAck)(channel)
      }

    case req @ ConfirmSelect =>
      sender ! handleRequest(req){ () =>
        confirmSelect(channel)
      }

    case req @ WaitForConfirms(timeout) =>
      sender ! handleRequest(req){ () =>
        waitForConfirms(timeout)(channel)
      }

    case req @ WaitForConfirmsOrDie(timeout) =>
      sender ! handleRequest(req){ () =>
        waitForConfirmsOrDie(timeout)(channel)
      }

    case req @ AddConsumer(listener) =>
      sender ! handleRequest(req){ () =>
        addConsumer(listener)(channel)
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
