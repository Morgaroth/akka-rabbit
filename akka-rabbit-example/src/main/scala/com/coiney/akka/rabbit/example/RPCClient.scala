package com.coiney.akka.rabbit.example

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.coiney.akka.rabbit.RabbitFactory
import com.typesafe.config.ConfigFactory
import com.coiney.akka.rabbit.RPC
import com.coiney.akka.rabbit.messages._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object RPCClient extends App {
  import ExecutionContext.Implicits.global
  implicit val timeout: Timeout = 5.seconds

  implicit val system = ActorSystem("ProducerSystem")

  // load the configuration and initialize the RabbitFactory
  val cfg = ConfigFactory.load()
  val rabbit = RabbitFactory(cfg)

  // create the connection keeper and wait for it to be connected
  val connectionKeeper = rabbit.createConnection(Some("connection"))
  rabbit.waitForConnection(connectionKeeper)

  // create the RPC Client and wait for it to be connected
  val rpcClient = rabbit.createRPCClient(connectionKeeper, name = Some("rpc-client"))
  rabbit.waitForConnection(rpcClient)

  while(true) {
    val msg = scala.util.Random.nextString(10)
    println(s"sending request: $msg")
    (rpcClient ? RPC.Request(List(Publish("", "my_queue", msg.getBytes("UTF-8"))), 1)).mapTo[RPC.Response].map(response => {
      // we expect 1 delivery
      val delivery = response.handleDeliveries.head
      println("response : " + new String(delivery.body))
    })
    Thread.sleep(1000)
  }

}
