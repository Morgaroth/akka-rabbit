package com.coiney.akka.rabbit.example

import akka.actor.ActorSystem

import com.coiney.akka.rabbit._
import com.coiney.akka.rabbit.protocol._


class ExclamationProcessor extends RabbitRPCProcessor {
  override def process(hd: HandleDelivery): RabbitRPCResult = {
    val req = new String(hd.body)
    println(s"Received: $req")
    val res = s"$req!"
    Thread.sleep(scala.util.Random.nextInt(2000))
    RabbitRPCResult(Some(res.getBytes("UTF-8")))
  }

  override def recover(hd: HandleDelivery, cause: Throwable): RabbitRPCResult =
    RabbitRPCResult(Some(s"Processor error: ${cause.getMessage}".getBytes("UTF-8")))
}


object RPCServerExample extends App {

  implicit val system = ActorSystem("ProducerSystem")

  // Add system shutdown hook
  sys.addShutdownHook(system.shutdown())

  val rabbitSystem = RabbitSystem()

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbitSystem waitFor rabbitSystem.createConnection("connection")

  // create the RPC Server and wait for it to be connected
  val rpcServer = rabbitSystem waitFor rabbitSystem.createRPCServer(connectionKeeper, new ExclamationProcessor(), QueueConfig("my_queue", durable = false, exclusive = false, autoDelete = true), "rpc-server")

}
