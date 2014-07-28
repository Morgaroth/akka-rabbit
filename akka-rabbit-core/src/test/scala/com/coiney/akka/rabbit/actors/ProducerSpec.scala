package com.coiney.akka.rabbit.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, ImplicitSender, TestKit}
import com.coiney.akka.pattern.Observable.RegisterObserver
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import com.coiney.akka.rabbit.protocol
import com.coiney.akka.rabbit.protocol._
import scala.concurrent.duration._
import scala.util.Random


@RunWith(classOf[JUnitRunner])
class ProducerSpec(_actorSystem: ActorSystem) extends TestKit(_actorSystem)
                                              with ImplicitSender
                                              with WordSpecLike
                                              with Matchers
                                              with BeforeAndAfterEach
                                              with BeforeAndAfterAll
                                              with RabbitSpec
                                              with ChannelKeeperSpec {

  def this() = this(ActorSystem("producer-spec"))

  var producer: ActorRef = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    producer = rabbitSystem waitFor rabbitSystem.createProducer(connectionKeeper)
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }

  "A producer" should {


    "be able to declare, bind, publish to, purge and delete a queue" in {
      // declare a queue. Messages on it should be 0
      val queue = randomQueue
      producer ! DeclareQueue(queue)
      val Success(_, Some(declareOk1: com.rabbitmq.client.AMQP.Queue.DeclareOk)) = receiveOne(1.second)
      declareOk1.getMessageCount should be (0)

      // bind the queue to test-routing-key on amq.direct
      producer ! BindQueue(queue.name, "amq.direct", "test-routing-key")
      expectMsgClass(classOf[protocol.Success])

      // publish a message on amq.direct with test-routing-key
      val msg = Random.nextString(10)
      producer ! Publish("amq.direct", "test-routing-key", msg.getBytes)
      expectMsgClass(classOf[protocol.Success])

      // declare passive & check that there is one message in the queue
      producer ! DeclareQueuePassive(queue.name)
      val Success(_, Some(declareOk2: com.rabbitmq.client.AMQP.Queue.DeclareOk)) = receiveOne(1.second)
      declareOk2.getMessageCount should be (1)

      // purge the queue
      producer ! PurgeQueue(queue.name)
      expectMsgClass(classOf[protocol.Success])

      // check that there are no more messages in the queue
      producer ! DeclareQueue(queue)
      val Success(_, Some(declareOk3: com.rabbitmq.client.AMQP.Queue.DeclareOk)) = receiveOne(1.second)
      declareOk3.getMessageCount should be (0)

      // delete the queue, and check that the count is 0
      producer ! DeleteQueue(queue.name)
      val Success(_, Some(deleteOk: com.rabbitmq.client.AMQP.Queue.DeleteOk)) = receiveOne(1.second)
      deleteOk.getMessageCount should be (0)
    }


    "be able to declare, bind, and delete an exchange" in {

      // declare 2 exchanges
      val exchange1 = randomTopicExchange
      producer ! DeclareExchange(exchange1)
      expectMsgClass(classOf[Success])
      val exchange2 = randomTopicExchange
      producer ! DeclareExchange(exchange2)
      expectMsgClass(classOf[Success])

      // bind exchange
      producer ! BindExchange(exchange1.name, exchange2.name, "routing-key")
      expectMsgClass(classOf[Success])

      // declare one of the existing exchanges passively
      producer ! DeclareExchangePassive(exchange1.name)
      expectMsgClass(classOf[Success])

      // delete the 2 exchanges
      producer ! DeleteExchange(exchange1.name)
      expectMsgClass(classOf[Success])
      producer ! DeleteExchange(exchange2.name)
      expectMsgClass(classOf[Success])
    }


    "be able to publish in a transaction" in {
      // declare a queue. Messages on it should be 0
      val queue = randomQueue
      producer ! DeclareQueue(queue)
      val Success(_, Some(declareOk1: com.rabbitmq.client.AMQP.Queue.DeclareOk)) = receiveOne(1.second)
      declareOk1.getMessageCount should be (0)

      // bind the queue to test-routing-key on amq.direct
      producer ! BindQueue(queue.name, "amq.direct", "test-routing-key")
      expectMsgClass(classOf[protocol.Success])

      // publish a message on amq.direct with test-routing-key
      val msg1 = Random.nextString(10)
      val msg2 = Random.nextString(10)
      val publish1 = Publish("amq.direct", "test-routing-key", msg1.getBytes)
      val publish2 = Publish("amq.direct", "test-routing-key", msg2.getBytes)
      producer ! Transaction(List(publish1, publish2))
      expectMsgClass(classOf[protocol.Success])

      // declare passive & check that there are 2 messages in the queue
      producer ! DeclareQueuePassive(queue.name)
      val Success(_, Some(declareOk: com.rabbitmq.client.AMQP.Queue.DeclareOk)) = receiveOne(1.second)
      declareOk.getMessageCount should be (2)

      // delete the queue, and check that the count is 2
      producer ! DeleteQueue(queue.name)
      val Success(_, Some(deleteOk: com.rabbitmq.client.AMQP.Queue.DeleteOk)) = receiveOne(1.second)
      deleteOk.getMessageCount should be (2)
    }



    "be a watchingObservable and inform its observers when it's connected" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      probe1.send(producer, RegisterObserver(probe1.ref))
      probe2.send(producer, RegisterObserver(probe2.ref))
      probe1.expectMsg(ChannelKeeper.Connected)
      probe2.expectMsg(ChannelKeeper.Connected)

      producer ! DeclareQueuePassive("invalid-queue")

      probe1.expectMsg(ChannelKeeper.Disconnected)
      probe2.expectMsg(ChannelKeeper.Disconnected)
    }


    "respond with a Failure, if a rabbitmq instruction goes wrong " in {
      // we're passively declaring a queue that doesn't exist -> fails
      producer ! DeclareQueuePassive("invalid-queue")
      expectMsgClass(classOf[protocol.Failure])
    }


  }

}
