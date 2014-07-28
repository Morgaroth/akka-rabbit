package com.coiney.akka.rabbit.actors

import akka.actor.Props

import com.coiney.akka.rabbit.ChannelConfig
import com.coiney.akka.rabbit.protocol.RabbitRequest


object Producer {
  def apply(channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): Producer =
    new Producer(channelConfig, provision) with AMQPRabbitFunctions

  def props(channelConfig: Option[ChannelConfig] = None, provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]): Props =
    Props(Producer(channelConfig, provision))
}


class Producer(channelConfig: Option[ChannelConfig] = None,
               provision: Seq[RabbitRequest] = Seq.empty[RabbitRequest]) extends ChannelKeeper(channelConfig) {
  this: RabbitFunctions =>
}
