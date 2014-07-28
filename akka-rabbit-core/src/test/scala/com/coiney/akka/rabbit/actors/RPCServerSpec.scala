package com.coiney.akka.rabbit.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
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


  "An RPCServer" should {

    "correctly respond to clients in a request-single response configuration" in {
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
      val consumeQueue = randomQueue

      // create the server
      rabbitSystem waitFor rabbitSystem.createRPCServer(connectionKeeper, rabbitRPCProcessor, consumeQueue)

      // create the client
      val rpcClient = rabbitSystem waitFor rabbitSystem.createRPCClient(connectionKeeper)

      forAll { (s: String) =>
        rpcClient ! RabbitRPCRequest(List(Publish("", consumeQueue.name, s.getBytes("UTF-8"))), 1)

        val RabbitRPCResponse(List(HandleDelivery(_, _, _, body))) = receiveOne(2.seconds)
        new String(body) should be(s.reverse)
      }
    }

  }

}
