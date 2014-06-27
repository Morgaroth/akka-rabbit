package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class WatchingObservableAckTestActor extends Actor with WatchingObservable {
  override def receive: Actor.Receive = observeReceive(Some("Ack"), None)
}

class WatchingObservableNoAckTestActor extends Actor with WatchingObservable {
  override def receive: Actor.Receive = observeReceive(None, None)
}


object ObservableSpec {

}


class ObservableSpec(_system: ActorSystem) extends TestKit(_system)
                                           with ImplicitSender
                                           with WordSpecLike
                                           with Matchers
                                           with BeforeAndAfterAll {

  def this() = this(ActorSystem("ObservableSpec"))

  override def afterAll(): Unit = {
    system.shutdown()
  }

  "A WatchingObservable" should {

    "Allow to register an observer" in {
      val actor = TestActorRef[WatchingObservableNoAckTestActor]
      actor ! Observable.RegisterObserver(testActor)
      actor.underlyingActor.observers should contain (testActor)
    }

    "Allow to register multiple observers" in {
      val actor = TestActorRef[WatchingObservableNoAckTestActor]
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      actor ! Observable.RegisterObserver(probe1.ref)
      actor ! Observable.RegisterObserver(probe2.ref)
      actor.underlyingActor.observers.size should be (2)
    }

    "Register an observer only once" in {
      val actor = TestActorRef[WatchingObservableNoAckTestActor]
      actor ! Observable.RegisterObserver(testActor)
      actor ! Observable.RegisterObserver(testActor)
      actor.underlyingActor.observers.size should be (1)
    }

    "send the events to the listener(s)" in {
      val actor = TestActorRef[WatchingObservableNoAckTestActor]
      actor ! Observable.RegisterObserver(testActor)
      actor.underlyingActor.sendEvent("fibs")
      expectMsg("fibs")
    }

    "send a message to it's observer upon registration if defined" in {
      val actor = TestActorRef[WatchingObservableAckTestActor]
      actor ! Observable.RegisterObserver(testActor)
      expectMsg("Ack")
    }

  }

}
