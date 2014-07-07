package com.coiney.akka.rabbit.actors

import akka.actor.Props

import com.coiney.akka.rabbit.ChannelConfig


object Producer {
  def apply(channelConfig: Option[ChannelConfig] = None): Producer = new Producer(channelConfig) with AMQPRabbitFunctions

  def props(channelConfig: Option[ChannelConfig] = None): Props = Props(Producer(channelConfig))
}


class Producer(channelConfig: Option[ChannelConfig] = None) extends ChannelKeeper(channelConfig) {
  this: RabbitFunctions =>
}
