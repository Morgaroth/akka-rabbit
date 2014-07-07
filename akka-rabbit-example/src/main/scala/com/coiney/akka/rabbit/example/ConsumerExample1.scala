package com.coiney.akka.rabbit.example

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

import com.coiney.akka.rabbit.{QueueConfig, RabbitFactory}
import com.coiney.akka.rabbit.messages._


object ConsumerExample1 extends App {

  implicit val system = ActorSystem("ConsumerSystem")

  // load the configuration and initialize the RabbitFactory
  val cfg = ConfigFactory.load()
  val rabbit = RabbitFactory(cfg)

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbit.createConnection(Some("connection"))
  rabbit.waitForConnection(connectionKeeper)

  // create the producer and wait for it to be connected
  val consumeActor = system.actorOf(Props(classOf[ConsumeActor]))
  val consumer = rabbit.createConsumer(connectionKeeper, consumeActor, name = Some("consumer"))
  rabbit.waitForConnection(consumer)

  // consume the queue
  consumer ! ConsumeQueue(QueueConfig("my_queue"))

  // shutdown the system
  Thread.sleep(1000)
  system.shutdown()
}
