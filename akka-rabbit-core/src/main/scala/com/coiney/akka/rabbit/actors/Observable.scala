package com.coiney.akka.rabbit.actors

import akka.actor.{Actor, ActorRef}


object Observable {
  case class RegisterObserver(observer: ActorRef)
  case class DeregisterObserver(observer: ActorRef)
}

trait Observable {
  def sendEvent[T](event: T): Unit
  def observerReceive(onRegister: Option[Any] = None, onDeregister: Option[Any] = None): Actor.Receive
}

trait BasicObservable {
  this: Actor =>
  import com.coiney.akka.rabbit.actors.Observable._

  var observers: Vector[ActorRef] = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = observers.foreach(_ ! event)

  def observeReceive(onRegister: Option[Any] = None, onDeregister: Option[Any] = None): Actor.Receive = {
    case RegisterObserver(observer) if !observers.contains(observer) =>
      observers = observers :+ observer
      onRegister.foreach(observer ! _)
    case DeregisterObserver(observer) =>
      observers = observers.filterNot(_ == observer)
      onDeregister.foreach(observer ! _)
  }

}

trait WatchingObservable {
  this: Actor =>
  import com.coiney.akka.rabbit.actors.Observable._

  var observers: Vector[ActorRef] = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = observers.foreach(_ ! event)

  def observeReceive(onRegister: Option[Any] = None, onDeregister: Option[Any] = None): Actor.Receive = {
    case RegisterObserver(observer) if !observers.contains(observer) =>
      registerObserver(observer, onRegister)
    case DeregisterObserver(observer) =>
      deregisterObserver(observer, onDeregister)
  }

  def registerObserver(observer: ActorRef, onRegister: Option[Any] = None): Unit = {
    context.watch(observer)
    observers = observers :+ observer
    onRegister.foreach(observer ! _)
  }

  def deregisterObserver(observer: ActorRef, onDeregister: Option[Any] = None): Unit = {
    context.unwatch(observer)
    observers = observers.filterNot(_ == observer)
    onDeregister.foreach(observer ! _)
  }

}