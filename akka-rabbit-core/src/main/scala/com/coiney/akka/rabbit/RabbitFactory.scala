package com.coiney.akka.rabbit

import java.util.concurrent.{CountDownLatch, ExecutorService}

import akka.actor._
import akka.pattern.ask
import com.coiney.akka.rabbit.messages.Request
import com.typesafe.config.Config
import com.rabbitmq.client.ConnectionFactory
import actors._

import com.coiney.akka.pattern.Observable

import scala.concurrent.Await
import scala.concurrent.duration._


trait RabbitFactory {
  this: RabbitConfiguration =>
  import com.coiney.akka.rabbit.messages._

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

  def createConnection(name: Option[String] = None): ActorRef =
    actorRefFactory.actorOf(actors.ConnectionKeeper.props(connectionFactory))

  def createProducer(connectionKeeper: ActorRef, channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request], name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val futureProducer = (connectionKeeper ? ConnectionKeeper.CreateChild(Producer.props(channelConfig, provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureProducer, timeout)
  }

  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: Option[ChannelConfig] = None, queueConfig: Option[QueueConfig] = None, name: Option[String] = None, autoAck: Boolean = false, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val provision: Seq[Request] = queueConfig match {
      case Some(cfg) => Seq(ConsumeQueue(cfg))
      case None      => Seq.empty[Request]
    }
    val futureConsumer = (connectionKeeper ? ConnectionKeeper.CreateChild(Consumer.props(listener, autoAck, channelConfig, provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureConsumer, timeout)
  }

  def createRPCServer(connectionKeeper: ActorRef, processor: RPC.Processor, channelConfig: Option[ChannelConfig] = None, queueConfig: Option[QueueConfig] = None, name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val provision: Seq[Request] = queueConfig match {
      case Some(cfg) => Seq(ConsumeQueue(cfg))
      case None      => Seq.empty[Request]
    }
    val futureRPCServer = (connectionKeeper ? ConnectionKeeper.CreateChild(RPCServer.props(processor, channelConfig, provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureRPCServer, timeout)
  }

  def createRPCClient(connectionKeeper: ActorRef, channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request], name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val futureRPCClient = (connectionKeeper ? ConnectionKeeper.CreateChild(RPCClient.props(channelConfig, provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureRPCClient, timeout)
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
