package com.coiney.akka.rabbit

import com.typesafe.config.Config


case class ChannelConfig (
  consumerPrefetchCount: Int,
  channelPrefetchCount: Int = 0
)


case class QueueConfig (
  name: String,
  durable: Boolean = false,
  exclusive: Boolean = false,
  autoDelete: Boolean = true,
  arguments: Map[String, AnyRef] = Map.empty[String, AnyRef]
)


case class ExchangeConfig (
  name: String,
  exchangeType: String,
  durable: Boolean = false,
  autoDelete: Boolean = false,
  arguments: Map[String, AnyRef] = Map.empty[String, AnyRef]
)