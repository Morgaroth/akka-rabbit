package com.coiney.akka.rabbit.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.gracefulStop
import akka.testkit.TestKit
import com.coiney.akka.rabbit.{RabbitSystem, ExchangeConfig, QueueConfig}
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random


trait ChannelKeeperSpec {
  this: TestKit with BeforeAndAfterEach =>

  def randomQueueName = s"queue.${Random.nextInt()}"
  def randomQueue = QueueConfig(randomQueueName, durable = false, exclusive = false, autoDelete = true)

  def randomExchangeName = s"exchange.${Random.nextInt()}"
  def randomDirectExchange = ExchangeConfig(randomExchangeName, "direct", durable = false, autoDelete = true)
  def randomTopicExchange = ExchangeConfig(randomExchangeName, "topic", durable = false, autoDelete = true)

  var rabbitSystem: RabbitSystem = _
  var connectionKeeper: ActorRef = _

  override def beforeEach(): Unit = {
    rabbitSystem = RabbitSystem()
    connectionKeeper = rabbitSystem waitFor rabbitSystem.createConnection()
  }

  override def afterEach(): Unit = {
    Await.result(gracefulStop(connectionKeeper, 4.seconds), 5.seconds)
  }

}
