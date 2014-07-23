package com.coiney.akka.rabbit.actors

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class ConnectionKeeperSpec(_actorSystem: ActorSystem) extends TestKit(_actorSystem)
                                                      with WordSpecLike
                                                      with Matchers
                                                      with BeforeAndAfterAll
                                                      with RabbitSpec {

  def this() = this(ActorSystem("connection-keeper-spec"))

  def TestConnectionKeeperRef(): TestActorRef[ConnectionKeeper] =
    TestActorRef(ConnectionKeeper(settings))


  "A disconnected ConnectionKeeper actor" should {

    "create and return a child actor" in {
      val probe = TestProbe()
      val connectionKeeper = TestConnectionKeeperRef()

      // the connectionKeeper should have no children to start with
      connectionKeeper.underlyingActor.context.children should have size (0)

      // create a child actor, and get it back
      probe.send(connectionKeeper, ConnectionKeeper.CreateChild(Props(new EchoActor), None))
      probe.expectMsgClass(2.seconds, classOf[ActorRef])

      // the connectionkeeper should now have one child
      connectionKeeper.underlyingActor.context.children should have size (1)
    }


    "connect to rabbitmq, and inform it's observers" in {
      val observerProbe = TestProbe()
      val connectionKeeper = TestConnectionKeeperRef()

      // start without a connection
      connectionKeeper.underlyingActor.connection should be (None)

      // register the observerProbe as observer, and we expect no message
      connectionKeeper ! com.coiney.akka.pattern.Observable.RegisterObserver(observerProbe.ref)

      // connect -> there should be a connection and the observer should be informed
      connectionKeeper ! ConnectionKeeper.Connect
      connectionKeeper.underlyingActor.connection should not be (None)
      observerProbe.expectMsg(2.seconds, ConnectionKeeper.Connected)
    }

  }


  "A connected ConnectionKeeper actor" should {

    "inform new observers that it is connected" in {
      val observerProbe = TestProbe()
      val connectionKeeper = TestConnectionKeeperRef()

      // connect
      connectionKeeper ! ConnectionKeeper.Connect

      // register the observerProbe as observer, and we should immediately be informed of it being connected
      connectionKeeper ! com.coiney.akka.pattern.Observable.RegisterObserver(observerProbe.ref)
      observerProbe.expectMsg(2.seconds, ConnectionKeeper.Connected)
    }


    "create and return a child actor" in {
      val probe = TestProbe()
      val connectionKeeper = TestConnectionKeeperRef()

      // connect
      connectionKeeper ! ConnectionKeeper.Connect

      // create a child actor
      probe.send(connectionKeeper, ConnectionKeeper.CreateChild(Props(new EchoActor), None))

      probe.expectMsgClass(2.seconds, classOf[ActorRef])
    }


    "create and return a channel" in {
      val probe = TestProbe()
      val connectionKeeper = TestConnectionKeeperRef()

      // connect
      connectionKeeper ! ConnectionKeeper.Connect

      // Request a channel
      probe.send(connectionKeeper, ConnectionKeeper.GetChannel)

      probe.expectMsgClass(2.seconds, classOf[ChannelKeeper.HandleChannel])
    }


    "create and return many different channels" in {
      val connectionKeeper = TestConnectionKeeperRef()

      // connect
      connectionKeeper ! ConnectionKeeper.Connect

      // request many channels
      val nrOfActors = 50
      for (i <- 1 to nrOfActors) {
        val probe = TestProbe()
        probe.send(connectionKeeper, ConnectionKeeper.GetChannel)
        probe.expectMsgClass(2.seconds, classOf[ChannelKeeper.HandleChannel])
      }
    }

  }

}
