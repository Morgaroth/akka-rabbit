package com.coiney.akka.rabbit.actors

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.coiney.akka.rabbit.protocol._
import org.scalacheck.Gen.Choose
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class RPCServerSpec(_actorSystem: ActorSystem) extends TestKit(_actorSystem)
                                               with ImplicitSender
                                               with WordSpecLike
                                               with Matchers
                                               with BeforeAndAfterAll
                                               with BeforeAndAfterEach
                                               with GeneratorDrivenPropertyChecks
                                               with RabbitSpec
                                               with ChannelKeeperSpec {

  def this() = this(ActorSystem("RPCServerSpec"))

  val rabbitRPCProcessor = new RabbitRPCProcessor {
    override def process(hd: HandleDelivery): RabbitRPCResult = {
      val message = new String(hd.body)
      val result = message.reverse
      RabbitRPCResult(Some(result.getBytes("UTF-8")))
    }

    override def recover(hd: HandleDelivery, cause: Throwable): RabbitRPCResult = {
      RabbitRPCResult(Some(cause.getMessage.getBytes("UTF-8")))
    }
  }


  "An RPCServer" should {

    "correctly respond to clients in a request-single response configuration (single requester)" in {
      val rpcQueue = randomQueue

      // create the server
      rabbitSystem waitFor rabbitSystem.createRPCServer(connectionKeeper, rabbitRPCProcessor, rpcQueue)

      // create the client
      val rpcClient = rabbitSystem waitFor rabbitSystem.createRPCClient(connectionKeeper)

      forAll { (s: String) =>
        rpcClient ! RabbitRPCRequest(List(Publish("", rpcQueue.name, s.getBytes("UTF-8"))), 1)

        val RabbitRPCResponse(List(HandleDelivery(_, _, _, body))) = receiveOne(2.seconds)
        new String(body) should be(s.reverse)
      }
    }


    "correctly respond to clients in a request-single response configuration (multiple requesters)" in {
      val rpcQueue = randomQueue
      val probe1 = TestProbe()
      val probe2 = TestProbe()

      // create the server
      rabbitSystem waitFor rabbitSystem.createRPCServer(connectionKeeper, rabbitRPCProcessor, rpcQueue)

      // create the clients
      val rpcClient = rabbitSystem waitFor rabbitSystem.createRPCClient(connectionKeeper)

      forAll { (s: String) =>
        probe1.send(rpcClient, RabbitRPCRequest(List(Publish("", rpcQueue.name, s.getBytes("UTF-8"))), 1))
        probe2.send(rpcClient, RabbitRPCRequest(List(Publish("", rpcQueue.name, s.getBytes("UTF-8"))), 1))

        val RabbitRPCResponse(List(HandleDelivery(_, _, _, body1))) = probe1.receiveOne(2.seconds)
        val RabbitRPCResponse(List(HandleDelivery(_, _, _, body2))) = probe2.receiveOne(2.seconds)
        new String(body1) should be(s.reverse)
        new String(body2) should be(s.reverse)
      }

    }

  }

}
