package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorRef, Props}
import com.rabbitmq.client.{DefaultConsumer, Envelope, AMQP, Channel}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}


object Consumer {
  def props(listener: ActorRef, autoAck: Boolean = false): Props = Props(classOf[Consumer], listener, autoAck)
}


class Consumer(listener: ActorRef, autoAck: Boolean = false) extends ChannelKeeper {
  import com.coiney.akka.rabbit.messages._

  var consumer: Option[DefaultConsumer] = None

  override def connected(channel: Channel, handler: ActorRef) = consumerConnected(channel, handler) orElse super.connected(channel, handler)

  def consumerConnected(channel: Channel, handler: ActorRef): Actor.Receive = {
    case req @ ConsumeQueue(name, durable, exclusive, autoDelete, arguments) => consumer match {
      case None    => log.debug("Channel is not a consumer.")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          channel.queueDeclare(name, durable, exclusive, autoDelete, arguments)
          val consumerTag = channel.basicConsume(name, autoAck, c)
          log.debug(s"Consuming using $consumerTag.")
          consumerTag
        }
    }

    case req @ ConsumeBinding(exchangeName, exchangeType, queueName, routingKey, exchangeDurable, exchangeAutoDelete, queueDurable, queueExclusive, queueAutoDelete, exchangeArgs, queueArgs, bindingArgs) => consumer match {
      case None    => log.debug("Channel is not a consumer.")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          channel.exchangeDeclare(exchangeName, exchangeType, exchangeDurable, exchangeAutoDelete, exchangeArgs)
          channel.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, queueArgs)
          channel.queueBind(queueName, exchangeName, routingKey, exchangeArgs)
          val consumerTag = channel.basicConsume(queueName, autoAck, c)
          log.debug(s"Consuming using $consumerTag.")
          consumerTag
        }
    }

    case req @ CancelConsume(consumerTag) => consumer match {
      case None    => log.debug("Channel is not a consumer.")
      case Some(c) =>
        sender ! handleRequest(req){ () =>
          channel.basicCancel(consumerTag)
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

  override def channelCallback(channel: Channel): Unit = {
    super.channelCallback(channel)
    consumer = Some(
      new DefaultConsumer(channel){
        override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit =
          listener ! HandleDelivery(consumerTag, envelope, properties, body)

        override def handleCancel(consumerTag: String): Unit =
          listener ! HandleCancel(consumerTag)
      }
    )
  }

}
