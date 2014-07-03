package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{AMQP, Channel, _}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}


private[rabbit] object ChannelHandler {
  def props(channel: Channel): Props = Props(classOf[ChannelHandler], channel)
}


private[rabbit] class ChannelHandler(channel: Channel) extends Actor
                                                        with ActorLogging {
  import com.coiney.akka.rabbit.messages._

  override def postStop(): Unit = Try {
    channel.close()
  }

  override def receive: Actor.Receive = {

    case req @ AddConfirmListener(listener) =>
      sender ! handleRequest(req){ () =>
        channel.addConfirmListener(new ConfirmListener {
          override def handleAck(deliveryTag: Long, multiple: Boolean): Unit =
            listener ! HandleAck(deliveryTag, multiple)

          override def handleNack(deliveryTag: Long, multiple: Boolean): Unit =
            listener ! HandleNack(deliveryTag, multiple)
        })
      }

    case req @ AddReturnListener(listener) =>
      sender ! handleRequest(req){ () =>
        channel.addReturnListener(new ReturnListener {
          override def handleReturn(replyCode: Int, replyText: String, exchange: String, routingKey: String, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
            listener ! HandleReturn(replyCode, replyText, exchange, routingKey, properties, body)
        })
      }

    case req @ AddShutdownListener(listener) =>
      sender ! handleRequest(req){ () =>
        channel.addShutdownListener(new ShutdownListener {
          override def shutdownCompleted(cause: ShutdownSignalException): Unit =
            listener ! HandleShutdown(cause)
        })
      }

    case req @ DeclareQueue(name, durable, exclusive, autoDelete, arguments) =>
      sender ! handleRequest(req){ () =>
        channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)
      }

    case req @ DeclareQueuePassive(name) =>
      sender ! handleRequest(req){ () =>
        channel.queueDeclarePassive(name)
      }

    case req @ DeleteQueue(name, ifUnused, ifEmpty) =>
      sender ! handleRequest(req){ () =>
        channel.queueDelete(name, ifUnused, ifEmpty)
      }

    case req @ PurgeQueue(name) =>
      sender ! handleRequest(req){ () =>
        channel.queuePurge(name)
      }

    case req @ BindQueue(name, exchange, routingKey, arguments) =>
      sender ! handleRequest(req){ () =>
        channel.queueBind(name, exchange, routingKey, arguments)
      }

    case req @ UnbindQueue(name, exchange, routingKey) =>
      sender ! handleRequest(req){ () =>
        channel.queueUnbind(name, exchange, routingKey)
      }

    case req @ DeclareExchange(name, exchangeType, durable, autoDelete, arguments) =>
      sender ! handleRequest(req){ () =>
        channel.exchangeDeclare(name, exchangeType, durable, autoDelete, arguments)
      }

    case req @ DeclareExchangePassive(name) =>
      sender ! handleRequest(req){ () =>
        channel.exchangeDeclarePassive(name)
      }

    case req @ DeleteExchange(name) =>
      sender ! handleRequest(req){ () =>
        channel.exchangeDelete(name)
      }

    case req @ BindExchange(destination, source, routingKey, arguments) =>
      sender ! handleRequest(req){ () =>
        channel.exchangeBind(destination, source, routingKey, arguments)
      }

    case req @ UnbindExchange(destination, source, routingKey) =>
      sender ! handleRequest(req){ () =>
        channel.exchangeUnbind(destination, source, routingKey)
      }

    case req @ Publish(exchange, routingKey, body, mandatory, immediate, properties) =>
      log.debug(s"Publishing $req")
      val props = properties.getOrElse(new AMQP.BasicProperties.Builder().build())
      sender ! handleRequest(req){ () =>
        channel.basicPublish(exchange, routingKey, mandatory, immediate, props, body)
      }

    case req @ Transaction(pubs) =>
      sender ! handleRequest(req){ () =>
        channel.txSelect()
        pubs.foreach{ pub =>
          val props = pub.properties.getOrElse(new AMQP.BasicProperties.Builder().build())
          channel.basicPublish(pub.exchange, pub.routingKey, pub.mandatory, pub.immediate, props, pub.body)
        }
        channel.txCommit()
      }

    case req @ Ack(deliveryTag) =>
      sender ! handleRequest(req){ () =>
        channel.basicAck(deliveryTag, false)
      }

    case req @ Reject(deliveryTag, requeue) =>
      sender ! handleRequest(req){ () =>
        channel.basicReject(deliveryTag, false)
      }

    case req @ Get(queue, autoAck) =>
      sender ! handleRequest(req){ () =>
        channel.basicGet(queue, autoAck)
      }

    case req @ ConfirmSelect =>
      sender ! handleRequest(req){ () =>
        channel.confirmSelect()
      }

    case req @ WaitForConfirms(timeout) =>
      sender ! handleRequest(req){ () =>
        timeout match {
          case None    => channel.waitForConfirms()
          case Some(t) => channel.waitForConfirms(t.toMillis)
        }
      }

    case req @ WaitForConfirmsOrDie(timeout) =>
      sender ! handleRequest(req){ () =>
        timeout match {
          case None    => channel.waitForConfirms()
          case Some(t) => channel.waitForConfirms(t.toMillis)
        }
      }

    case req @ AddConsumer(listener) =>
      sender ! handleRequest(req){ () =>
        new DefaultConsumer(channel){
          override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
            listener ! HandleDelivery(consumerTag, envelope, properties, body)

          override def handleCancel(consumerTag: String): Unit =
            listener ! HandleCancel(consumerTag)
        }
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
