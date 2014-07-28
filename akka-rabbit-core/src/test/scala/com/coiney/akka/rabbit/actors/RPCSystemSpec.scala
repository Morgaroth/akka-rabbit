package com.coiney.akka.rabbit.actors

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.coiney.akka.rabbit.protocol._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class RPCSystemSpec(_actorSystem: ActorSystem) extends TestKit(_actorSystem)
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


  "An RPC System" when {

    "single server and single requester" should {

      "correctly respond in a request-single response configuration" in {
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

    }


    "single server and multiple requesters" should {

      "correctly respond to clients in a request-single response configuration" in {
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


    "single server and multiple clients" should {

      "correctly respond to clients in a request-single response configuration" in {
        val rpcQueue = randomQueue

        // create the server
        rabbitSystem waitFor rabbitSystem.createRPCServer(connectionKeeper, rabbitRPCProcessor, rpcQueue)

        // create the clients
        val rpcClient1 = rabbitSystem waitFor rabbitSystem.createRPCClient(connectionKeeper)
        val rpcClient2 = rabbitSystem waitFor rabbitSystem.createRPCClient(connectionKeeper)

        forAll { (s1: String, s2: String) =>
          rpcClient1 ! RabbitRPCRequest(List(Publish("", rpcQueue.name, s1.getBytes("UTF-8"))), 1)
          rpcClient2 ! RabbitRPCRequest(List(Publish("", rpcQueue.name, s2.getBytes("UTF-8"))), 1)

          val Seq(RabbitRPCResponse(List(HandleDelivery(_, _, _, body1))), RabbitRPCResponse(List(HandleDelivery(_, _, _, body2)))) = receiveN(2)
          val responses = List(new String(body1), new String(body2))
          responses should contain(s1.reverse)
          responses should contain(s2.reverse)
        }
      }

    }

  }

}
