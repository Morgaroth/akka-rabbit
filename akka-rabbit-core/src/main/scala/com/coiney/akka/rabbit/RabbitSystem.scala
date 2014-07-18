package com.coiney.akka.rabbit

import java.util.concurrent.CountDownLatch

import akka.actor.{Actor, Props, ActorRef, ActorRefFactory}
import akka.pattern.ask
import com.typesafe.config.{ConfigFactory, Config}

import com.coiney.akka.pattern.Observable
import com.coiney.akka.rabbit.actors._
import com.coiney.akka.rabbit.protocol.{RabbitRPCProcessor, ConsumeQueue, RabbitRequest}

import scala.concurrent.Await
import scala.concurrent.duration._


object RabbitSystem {

  def apply()(implicit actorRefFactory: ActorRefFactory): RabbitSystem =
    apply(None, None)(actorRefFactory)

  def apply(config: Config)(implicit actorRefFactory: ActorRefFactory): RabbitSystem =
    apply(Option(config), None)(actorRefFactory)

  def apply(config: Config, classLoader: ClassLoader)(implicit actorRefFactory: ActorRefFactory): RabbitSystem =
    apply(Option(config), Option(classLoader))(actorRefFactory)

  def apply(config: Option[Config], classLoader: Option[ClassLoader])(implicit actorRefFactory: ActorRefFactory): RabbitSystem = {
    val cl = classLoader.getOrElse(findClassLoader())
    val rabbitConfig = config.getOrElse(ConfigFactory.load(cl))
    new RabbitSystem(rabbitConfig, cl)(actorRefFactory)
  }

  class Settings(classLoader: ClassLoader, cfg: Config) {

    /**
     * The Config backing this RabbitSystem's Settings
     */
    final val config: Config = {
      val config = cfg.withFallback(ConfigFactory.load(classLoader))
      config.checkValid(ConfigFactory.defaultReference(classLoader), "rabbit")
      config
    }

    import config._

    final val host: String = getString("rabbit.host")
    final val port: Int = getInt("rabbit.port")
    final val username: String = getString("rabbit.username")
    final val password: String = getString("rabbit.password")
    final val virtualHost: String = getString("rabbit.virtual-host")
    final val connectionTimeout: Int = getInt("rabbit.connection-timeout")
    final val requestedChannelMax: Int = getInt("rabbit.requested-channel-max")
    final val requestedFrameMax: Int = getInt("rabbit.requested-frame-max")
    final val requestedHeartbeat: Int = getInt("rabbit.requested-heartbeat")

    /**
     * Returns the String representation of the Config that this Settings is backed by
     */
    override def toString: String = config.root.render
  }

  private[rabbit] def findClassLoader(): ClassLoader = {
    Option(Thread.currentThread.getContextClassLoader) getOrElse
      getClass.getClassLoader
  }

}

class RabbitSystem(rabbitConfig: Config, classLoader: ClassLoader)(implicit _actorRefFactory: ActorRefFactory) {

  import RabbitSystem._

  /**
   * The core settings extracted from the supplied configuration.
   */
  final val settings: Settings = new Settings(classLoader, rabbitConfig)

  private val actorRefFactory = _actorRefFactory

  // Connection constructors
  def createConnection(): ActorRef = actorRefFactory.actorOf(ConnectionKeeper.props(settings))
  def createConnection(name: String): ActorRef = actorRefFactory.actorOf(ConnectionKeeper.props(settings), name)

  // Producer constructors
  def createProducer(connectionKeeper: ActorRef): ActorRef =
    createProducer(connectionKeeper, None)
  def createProducer(connectionKeeper: ActorRef, name: String): ActorRef =
    createProducer(connectionKeeper, name = Some(name))
  def createProducer(connectionKeeper: ActorRef, channelConfig: ChannelConfig): ActorRef =
    createProducer(connectionKeeper, Some(channelConfig))
  def createProducer(connectionKeeper: ActorRef, channelConfig: ChannelConfig, name: String): ActorRef =
    createProducer(connectionKeeper, Some(channelConfig), name = Some(name))
  def createProducer(connectionKeeper: ActorRef, channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest], name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val futureProducer = (connectionKeeper ? ConnectionKeeper.CreateChild(Producer.props(channelConfig, provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureProducer, timeout)
  }

  // Consumer constructors
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, name: String): ActorRef =
    createConsumer(connectionKeeper, listener, name = Some(name))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, name: String, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, name = Some(name), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, consumeQueueConfig: QueueConfig): ActorRef =
    createConsumer(connectionKeeper, listener, consumeQueueConfig = Some(consumeQueueConfig))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, consumeQueueConfig: QueueConfig, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, consumeQueueConfig = Some(consumeQueueConfig), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, consumeQueueConfig: QueueConfig, name: String): ActorRef =
    createConsumer(connectionKeeper, listener, consumeQueueConfig = Some(consumeQueueConfig), name = Some(name))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, consumeQueueConfig: QueueConfig, name: String, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, consumeQueueConfig = Some(consumeQueueConfig), name = Some(name), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, name: String): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), name = Some(name))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, name: String, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), name = Some(name), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, consumeQueueConfig: QueueConfig): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), Some(consumeQueueConfig))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, consumeQueueConfig: QueueConfig, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), Some(consumeQueueConfig), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, consumeQueueConfig: QueueConfig, name: String): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), Some(consumeQueueConfig), name = Some(name))
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: ChannelConfig, consumeQueueConfig: QueueConfig, name: String, autoAck: Boolean): ActorRef =
    createConsumer(connectionKeeper, listener, Some(channelConfig), Some(consumeQueueConfig), name = Some(name), autoAck = autoAck)
  def createConsumer(connectionKeeper: ActorRef, listener: ActorRef, channelConfig: Option[ChannelConfig] = None, consumeQueueConfig: Option[QueueConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest], name: Option[String] = None, autoAck: Boolean = false, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val preProvision: Seq[RabbitRequest] = consumeQueueConfig match {
      case Some(cfg) => Seq(ConsumeQueue(cfg))
      case None      => Seq.empty[RabbitRequest]
    }
    val futureConsumer = (connectionKeeper ? ConnectionKeeper.CreateChild(Consumer.props(listener, autoAck, channelConfig, preProvision ++ provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureConsumer, timeout)
  }

  // RPCServer constructors
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, name: String): ActorRef =
    createRPCServer(connectionKeeper, processor, name = Some(name))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, consumeQueueConfig: QueueConfig): ActorRef =
    createRPCServer(connectionKeeper, processor, consumeQueueConfig = Some(consumeQueueConfig))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, consumeQueueConfig: QueueConfig, name: String): ActorRef =
    createRPCServer(connectionKeeper, processor, consumeQueueConfig = Some(consumeQueueConfig), name = Some(name))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, channelConfig: ChannelConfig): ActorRef =
    createRPCServer(connectionKeeper, processor, Some(channelConfig))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, channelConfig: ChannelConfig, name: String): ActorRef =
    createRPCServer(connectionKeeper, processor, Some(channelConfig), name = Some(name))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, channelConfig: ChannelConfig, consumeQueueConfig: QueueConfig): ActorRef =
    createRPCServer(connectionKeeper, processor, Some(channelConfig), Some(consumeQueueConfig))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, channelConfig: ChannelConfig, consumeQueueConfig: QueueConfig, name: String): ActorRef =
    createRPCServer(connectionKeeper, processor, Some(channelConfig), Some(consumeQueueConfig), name = Some(name))
  def createRPCServer(connectionKeeper: ActorRef, processor: RabbitRPCProcessor, channelConfig: Option[ChannelConfig] = None, consumeQueueConfig: Option[QueueConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest], name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val preProvision: Seq[RabbitRequest] = consumeQueueConfig match {
      case Some(cfg) => Seq(ConsumeQueue(cfg))
      case None      => Seq.empty[RabbitRequest]
    }
    val futureRPCServer = (connectionKeeper ? ConnectionKeeper.CreateChild(RPCServer.props(processor, channelConfig, preProvision ++ provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureRPCServer, timeout)
  }

  // RPCClient constructors
  def createRPCClient(connectionKeeper: ActorRef, name: String): ActorRef =
    createRPCClient(connectionKeeper, name = Some(name))
  def createRPCClient(connectionKeeper: ActorRef, channelConfig: ChannelConfig): ActorRef =
    createRPCClient(connectionKeeper, Some(channelConfig))
  def createRPCClient(connectionKeeper: ActorRef, channelConfig: ChannelConfig, name: String): ActorRef =
    createRPCClient(connectionKeeper, Some(channelConfig), name = Some(name))
  def createRPCClient(connectionKeeper: ActorRef, channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest], name: Option[String] = None, timeout: FiniteDuration = 5000.millis): ActorRef = {
    val futureRPCClient = (connectionKeeper ? ConnectionKeeper.CreateChild(RPCClient.props(channelConfig, provision), name))(timeout).mapTo[ActorRef]
    Await.result(futureRPCClient, timeout)
  }

  def onConnected(actor: ActorRef, onConnected: () => Unit): Unit = {
    val callbackActor = actorRefFactory.actorOf(Props(new Actor{
      override def receive: Actor.Receive = {
        case ConnectionKeeper.Connected | ChannelKeeper.Connected =>
          onConnected()
          context.stop(self)
      }
    }))
    actor ! Observable.RegisterObserver(callbackActor)
  }

  def waitForConnectionOf(actors: ActorRef*): Unit = {
    val countDownLatch = new CountDownLatch(actors.size)
    actors.foreach(a => onConnected(a, () => countDownLatch.countDown()))
    countDownLatch.await()
  }

  def waitFor(constructor: => ActorRef): ActorRef = {
    val actor = constructor
    waitForConnectionOf(actor)
    actor
  }

}
