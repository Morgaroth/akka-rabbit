package com.coiney.akka.rabbit.actors

import akka.actor.{Props, Actor}
import akka.testkit.{TestActorRef, TestKit}
import com.coiney.akka.rabbit.RabbitSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll

trait RabbitSpec {
  this: TestKit with BeforeAndAfterAll =>

  override def afterAll(): Unit = {
    system.shutdown()
  }

  object EchoProbe {
    def apply(): EchoProbe = new EchoProbe
    def props(): Props = Props(EchoProbe())
  }

  class EchoProbe extends Actor {
    def receive: Actor.Receive = {
      case msg => sender ! msg
    }
  }

  val config = ConfigFactory.load()

  val settings = new RabbitSystem.Settings(RabbitSystem.findClassLoader(), config)

}
