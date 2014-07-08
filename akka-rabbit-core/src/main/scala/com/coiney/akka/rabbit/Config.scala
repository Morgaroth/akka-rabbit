package com.coiney.akka.rabbit

import com.typesafe.config.Config


object ConnectionConfig {
  def apply(config: Config): ConnectionConfig =
    new ConnectionConfig (
      host                = config.getString("host"),
      port                = config.getInt("port"),
      username            = config.getString("username"),
      password            = config.getString("password"),
      virtualHost         = config.getString("virtual-host"),
      connectionTimeout   = config.getInt("connection-timeout"),
      requestedChannelMax = config.getInt("requested-channel-max"),
      requestedFrameMax   = config.getInt("requested-frame-max"),
      requestedHeartbeat  = config.getInt("requested-heartbeat")
    )
}

case class ConnectionConfig (
  host: String,
  port: Int,
  username: String,
  password: String,
  virtualHost: String,
  connectionTimeout: Int = 0,
  requestedChannelMax: Int = 0,
  requestedFrameMax: Int = 0,
  requestedHeartbeat: Int = 0
)


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