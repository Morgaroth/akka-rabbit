package com.coiney.akka.rabbit.actors

import akka.actor.Actor
import akka.testkit.{TestActorRef, TestKit}
import com.coiney.akka.rabbit.RabbitSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll

trait RabbitSpec {
  this: TestKit with BeforeAndAfterAll =>

  override def afterAll(): Unit = {
    system.shutdown()
  }

  class EchoActor extends Actor {
    def receive: Actor.Receive = {
      case msg => sender ! msg
    }
  }

  val config = ConfigFactory.load()

  val settings = new RabbitSystem.Settings(RabbitSystem.findClassLoader(), config)

}
