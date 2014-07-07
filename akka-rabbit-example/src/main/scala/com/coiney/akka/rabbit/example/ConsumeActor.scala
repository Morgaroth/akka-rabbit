package com.coiney.akka.rabbit.example

import akka.actor.Actor

import com.coiney.akka.rabbit.messages.{HandleCancel, Ack, HandleDelivery}


class ConsumeActor extends Actor {
  override def receive: Actor.Receive = {
    case HandleDelivery(consumerTag, envelope, properties, body) =>
      println(new String(body))
      sender ! Ack(envelope.getDeliveryTag)
    case HandleCancel(consumerTag) => ()
  }
}