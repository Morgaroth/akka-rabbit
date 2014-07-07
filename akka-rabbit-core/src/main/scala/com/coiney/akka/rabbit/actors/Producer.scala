package com.coiney.akka.rabbit.actors

import akka.actor.Props

import com.coiney.akka.rabbit.ChannelConfig
import com.coiney.akka.rabbit.messages.Request


object Producer {
  def apply(channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): Producer =
    new Producer(channelConfig, provision) with AMQPRabbitFunctions

  def props(channelConfig: Option[ChannelConfig] = None, provision: Seq[Request] = Seq.empty[Request]): Props =
    Props(Producer(channelConfig, provision))
}


class Producer(channelConfig: Option[ChannelConfig] = None,
               provision: Seq[Request] = Seq.empty[Request]) extends ChannelKeeper(channelConfig) {
  this: RabbitFunctions =>
}
