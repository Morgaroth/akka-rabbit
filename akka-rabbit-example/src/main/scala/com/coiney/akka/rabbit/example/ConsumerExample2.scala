package com.coiney.akka.rabbit.example

import akka.actor.{Props, ActorSystem}

import com.coiney.akka.rabbit.{QueueConfig, RabbitSystem}


object ConsumerExample2 extends App {

  implicit val system = ActorSystem("ConsumerSystem")

  val rabbitSystem = RabbitSystem()

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbitSystem.createConnection("connection")
  rabbitSystem.waitForConnectionOf(connectionKeeper)

  // create the producer and wait for it to be connected
  val consumeActor = system.actorOf(Props(classOf[ConsumeActor]))
  val consumer = rabbitSystem waitFor rabbitSystem.createConsumer(connectionKeeper, consumeActor, QueueConfig("my_queue"), "consumer")

  // shutdown the system
  Thread.sleep(1000)
  system.shutdown()
}