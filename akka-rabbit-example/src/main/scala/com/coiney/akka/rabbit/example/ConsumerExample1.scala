package com.coiney.akka.rabbit.example

import akka.actor.{Props, ActorSystem}

import com.coiney.akka.rabbit.{QueueConfig, RabbitSystem}
import com.coiney.akka.rabbit.messages._


object ConsumerExample1 extends App {

  implicit val system = ActorSystem("ConsumerSystem")

  val rabbitSystem = RabbitSystem()

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbitSystem waitFor rabbitSystem.createConnection("connection")
  // // alternatively, you can also
  // val connectionKeeper = rabbitSystem.createConnection("connection")
  // rabbitSystem.waitForConnection(connectionKeeper)

  // create the producer and wait for it to be connected
  val consumeActor = system.actorOf(Props(classOf[ConsumeActor]))
  val consumer = rabbitSystem waitFor rabbitSystem.createConsumer(connectionKeeper, consumeActor, "consumer")
  // // Same here: alternatively, you can also
  // val consumer = rabbitSystem.createConsumer(connectionKeeper, consumeActor, "consumer")
  // rabbitSystem.waitForConnection(consumer)

  // consume the queue
  consumer ! ConsumeQueue(QueueConfig("my_queue"))

  // shutdown the system
  Thread.sleep(1000)
  system.shutdown()
}
