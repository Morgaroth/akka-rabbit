package com.coiney.akka.rabbit

import java.util.concurrent.{CountDownLatch, ExecutorService}

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}
import com.rabbitmq.client.ConnectionFactory
import actors._

import scala.concurrent.Await
import scala.concurrent.duration._


trait RabbitFactory {
  this: RabbitConfiguration =>

  def actorRefFactory: ActorRefFactory

  val connectionFactory = new ConnectionFactory()
  connectionFactory.setHost(config.host)
  connectionFactory.setPort(config.port)
  connectionFactory.setUsername(config.username)
  connectionFactory.setPassword(config.password)
  connectionFactory.setVirtualHost(config.virtualHost)
  connectionFactory.setConnectionTimeout(config.connectionTimeout)
  connectionFactory.setRequestedChannelMax(config.requestedChannelMax)
  connectionFactory.setRequestedFrameMax(config.requestedFrameMax)
  connectionFactory.setRequestedHeartbeat(config.requestedHeartbeat)

  def setSharedExecutor(executor: ExecutorService): Unit =
    connectionFactory.setSharedExecutor(executor)

  def createConnection(): ActorRef =
    actorRefFactory.actorOf(actors.ConnectionKeeper.props(connectionFactory))

  def createProducer(connectionKeeper: ActorRef, name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val futureProducer = (connectionKeeper ? ConnectionKeeper.CreateChild(Producer.props(), name))(timeout).mapTo[ActorRef]
    Await.result(futureProducer, timeout)
  }

  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, name: Option[String] = None, autoAck: Boolean = false, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val futureProducer = (connectionKeeper ? ConnectionKeeper.CreateChild(Consumer.props(listener, autoAck), name))(timeout).mapTo[ActorRef]
    Await.result(futureProducer, timeout)
  }

  def onConnected(actor: ActorRef, onConnected: () => Unit): Unit = {
    val z = actorRefFactory.actorOf(Props(new Actor{
      override def receive: Actor.Receive = {
        case ConnectionKeeper.Connected | ChannelKeeper.Connected =>
          onConnected()
          context.stop(self)
      }
    }))
    actor ! Observable.RegisterObserver(z)
  }

  def waitForConnection(actors: ActorRef*): Unit = {
    val countDownLatch = new CountDownLatch(actors.size)
    actors.foreach(a => onConnected(a, () => countDownLatch.countDown()))
    countDownLatch.await()
  }

}

object RabbitFactory {
  def apply(cfg: Config)(implicit _actorRefFactory: ActorRefFactory): RabbitFactory = new RabbitFactory with RabbitConfiguration {
    lazy val config: RabbitConfig = RabbitConfig(cfg)
    val actorRefFactory = _actorRefFactory
  }
}
