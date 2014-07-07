package com.coiney.akka.rabbit.example

import akka.actor.ActorSystem
import com.coiney.akka.rabbit.{QueueConfig, RabbitFactory}
import com.coiney.akka.rabbit.messages._
import com.typesafe.config.ConfigFactory

object Producer extends App {

  implicit val system = ActorSystem("ProducerSystem")

  // load the configuration and initialize the RabbitFactory
  val cfg = ConfigFactory.load()
  val rabbit = RabbitFactory(cfg)

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbit.createConnection(Some("connection"))
  rabbit.waitForConnection(connectionKeeper)

  // create the producer and wait for it to be connected
  val producer = rabbit.createProducer(connectionKeeper, name = Some("producer"))
  rabbit.waitForConnection(producer)

  // set the queue
  producer ! DeclareQueue(QueueConfig("my_queue"))

  // Send a message
  val msg = "512!!!"
  producer ! Publish("", "my_queue", msg.getBytes)

  // Shutdown the system
  Thread.sleep(1000)
  system.shutdown()
}
