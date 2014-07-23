package com.coiney.akka.rabbit.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.coiney.akka.rabbit.{ChannelConfig, QueueConfig}
import com.coiney.akka.rabbit.protocol._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.util.Random
import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class ConsumerSpec(_actorSystem: ActorSystem) extends TestKit(_actorSystem)
                                              with ImplicitSender
                                              with WordSpecLike
                                              with Matchers
                                              with BeforeAndAfterEach
                                              with BeforeAndAfterAll
                                              with RabbitSpec
                                              with ChannelKeeperSpec {

  def this() = this(ActorSystem("consumer-spec"))

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }

  "A consumer" should {

    "consume and auto ack messages in a queue" in {
      val listenerProbe = TestProbe()
      val producer = rabbitSystem waitFor rabbitSystem.createProducer(connectionKeeper)
      val consumer = rabbitSystem waitFor rabbitSystem.createConsumer(connectionKeeper, listenerProbe.ref, autoAck = true)
      val queue = QueueConfig("test.queue.97360", durable = false, exclusive = false, autoDelete = true)
      val msg = Random.nextString(10)

      // produce a message to the queue
      producer ! DeclareQueue(queue)
      expectMsgClass(classOf[Success])
      producer ! Publish("", queue.name, msg.getBytes("UTF-8"))
      expectMsgClass(classOf[Success])

      // consume from said queue and receive the message that was sent
      consumer ! ConsumeQueue(queue)
      val Success(_, Some(consumerTag: String)) = receiveOne(1.second)
      val HandleDelivery(_, _, _, rec: Array[Byte]) = listenerProbe.receiveOne(1.second)
      new String(rec) should be (msg)

      // The queue autoDeletes, so should be empty now
      producer ! DeclareQueue(queue)
      val Success(_, Some(declareOk: com.rabbitmq.client.AMQP.Queue.DeclareOk)) = receiveOne(1.second)
      declareOk.getMessageCount should be (0)

      // Delete the queue we used
      consumer ! CancelConsume(consumerTag)
      expectMsgClass(classOf[Success])
      producer ! DeleteQueue(queue.name)
      expectMsgClass(classOf[Success])
    }


    "consume and manually ack messages in a queue" in {
      val listenerProbe = TestProbe()
      val producer = rabbitSystem waitFor rabbitSystem.createProducer(connectionKeeper)
      val consumer = rabbitSystem waitFor rabbitSystem.createConsumer(connectionKeeper, listenerProbe.ref, ChannelConfig(1, 1), autoAck = false)
      val queue = QueueConfig("test.queue.82538", durable = false, exclusive = false, autoDelete = true)
      val msg1 = Random.nextString(10)
      val msg2 = Random.nextString(10)

      // produce messages to the queue
      producer ! DeclareQueue(queue)
      expectMsgClass(classOf[Success])
      producer ! Publish("", queue.name, msg1.getBytes("UTF-8"))
      expectMsgClass(classOf[Success])
      producer ! Publish("", queue.name, msg2.getBytes("UTF-8"))
      expectMsgClass(classOf[Success])

      // consume from said queue and receive the message that was sent
      consumer ! ConsumeQueue(queue)
      val Success(_, Some(consumerTag: String)) = receiveOne(1.second)
      val delivery1 = listenerProbe.expectMsgClass(classOf[HandleDelivery])
      new String(delivery1.body) should be (msg1)

      // Not receive the second message as long as we haven't acked
      listenerProbe.expectNoMsg(1.second)

      // ack the first message and receive the second
      consumer ! Ack(delivery1.envelope.getDeliveryTag)
      expectMsgClass(classOf[Success])
      val delivery2 = listenerProbe.expectMsgClass(classOf[HandleDelivery])
      new String(delivery2.body) should be (msg2)
      consumer ! Ack(delivery2.envelope.getDeliveryTag)

      // Delete the queue we used
      consumer ! CancelConsume(consumerTag)
      expectMsgClass(classOf[Success])
      producer ! DeleteQueue(queue.name)
      expectMsgClass(classOf[Success])
    }

  }

}
