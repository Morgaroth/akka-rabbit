package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorRef}


object Observable {
  case class RegisterObserver(observer: ActorRef)
  case class UnregisterObserver(observer: ActorRef)
}

trait Observable {
  def sendEvent[T](event: T): Unit
  def observerReceive(registerAck: Option[Any] = None, unregisterAck: Option[Any] = None): Actor.Receive
}

trait BasicObservable {
  this: Actor =>
  import com.coiney.akka.rabbit.actors.Observable._

  var observers: Vector[ActorRef] = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = observers.foreach(_ ! event)

  def observeReceive(registerAck: Option[Any] = None, unregisterAck: Option[Any] = None): Actor.Receive = {
    case RegisterObserver(observer) if !observers.contains(observer) =>
      observers = observers :+ observer
      registerAck.foreach(observer ! _)
    case UnregisterObserver(observer) =>
      observers = observers.filterNot(_ == observer)
      unregisterAck.foreach(observer ! _)
  }

}

trait WatchingObservable {
  this: Actor =>
  import com.coiney.akka.rabbit.actors.Observable._

  var observers: Vector[ActorRef] = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = observers.foreach(_ ! event)

  def observeReceive(registerAck: Option[Any] = None, unregisterAck: Option[Any] = None): Actor.Receive = {
    case RegisterObserver(observer) if !observers.contains(observer) =>
      registerObserver(observer, registerAck)
    case UnregisterObserver(observer) =>
      unregisterObserver(observer, unregisterAck)
  }

  def registerObserver(observer: ActorRef, ack: Option[Any] = None): Unit = {
    context.watch(observer)
    observers = observers :+ observer
    ack.foreach(observer ! _)
  }

  def unregisterObserver(observer: ActorRef, ack: Option[Any] = None): Unit = {
    context.unwatch(observer)
    observers = observers.filterNot(_ == observer)
    ack.foreach(observer ! _)
  }

}