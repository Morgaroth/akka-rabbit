package com.coiney.akka.rabbit.example

import akka.actor.ActorSystem

import com.coiney.akka.rabbit.{QueueConfig, RabbitSystem}
import com.coiney.akka.rabbit.messages._


object ProducerExample extends App {

  implicit val system = ActorSystem("ProducerSystem")

  val rabbitSystem = RabbitSystem()

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbitSystem waitFor rabbitSystem.createConnection("connection")

  // create the producer and wait for it to be connected
  val producer = rabbitSystem waitFor rabbitSystem.createProducer(connectionKeeper, "producer")

  // set the queue
  producer ! DeclareQueue(QueueConfig("my_queue"))

  // Send a message
  val msg = "512!!!"
  producer ! Publish("", "my_queue", msg.getBytes)

  // Shutdown the system
  Thread.sleep(1000)
  system.shutdown()
}
