package com.coiney.akka.rabbit.example

import akka.actor.{Props, Actor, ActorSystem}
import com.coiney.akka.rabbit.RabbitFactory
import com.coiney.akka.rabbit.messages._
import com.typesafe.config.ConfigFactory


class ConsumeActor extends Actor {
  override def receive: Actor.Receive = {
    case HandleDelivery(consumerTag, envelope, properties, body) =>
      println(new String(body))
      sender ! Ack(envelope.getDeliveryTag)
    case HandleCancel(consumerTag) => ()
  }
}


object Consumer extends App {

  implicit val system = ActorSystem("ConsumerSystem")

  // load the configuration and initialize the RabbitFactory
  val cfg = ConfigFactory.load()
  val rabbit = RabbitFactory(cfg)

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbit.createConnection(Some("connection"))
  rabbit.waitForConnection(connectionKeeper)

  // create the producer and wait for it to be connected
  val consumeActor = system.actorOf(Props(classOf[ConsumeActor]))
  val consumer = rabbit.createConsumer(connectionKeeper, consumeActor, Some("consumer"))
  rabbit.waitForConnection(consumer)

  // consume the queue
  consumer ! ConsumeQueue("my_queue")

  // shutdown the system
  Thread.sleep(1000)
  system.shutdown()
}
