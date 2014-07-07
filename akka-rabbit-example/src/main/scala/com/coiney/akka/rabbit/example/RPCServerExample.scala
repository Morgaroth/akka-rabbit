package com.coiney.akka.rabbit.example

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import com.coiney.akka.rabbit.RPC.Result
import com.coiney.akka.rabbit._
import com.coiney.akka.rabbit.messages.{ConsumeQueue, HandleDelivery}


class ExclamationProcessor extends RPC.Processor {
  override def process(hd: HandleDelivery): Result = {
    val req = new String(hd.body)
    println(s"Received: $req")
    val res = s"$req!"
    Thread.sleep(scala.util.Random.nextInt(2000))
    RPC.Result(Some(res.getBytes("UTF-8")))
  }

  override def recover(hd: HandleDelivery, cause: Throwable): Result =
    RPC.Result(Some(s"Processor error: ${cause.getMessage}".getBytes("UTF-8")))
}


object RPCServerExample extends App {

  implicit val system = ActorSystem("ProducerSystem")

  // load the configuration and initialize the RabbitFactory
  val cfg = ConfigFactory.load()
  val rabbit = RabbitFactory(cfg)

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbit.createConnection(Some("connection"))
  rabbit.waitForConnection(connectionKeeper)

  // create the RPC Server and wait for it to be connected
  val rpcServer = rabbit.createRPCServer(connectionKeeper, new ExclamationProcessor())
  rabbit.waitForConnection(rpcServer)

  // set the queue
  rpcServer ! ConsumeQueue(QueueConfig("my_queue", durable = false, exclusive = false, autoDelete = true))

  // Shutdown the system
  //Thread.sleep(1000)
  //system.shutdown()
}
