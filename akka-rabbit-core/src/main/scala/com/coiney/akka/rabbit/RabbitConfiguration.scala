package com.coiney.akka.rabbit

import com.typesafe.config.Config

trait RabbitConfiguration {
  def config: RabbitConfig
}

trait RabbitConfig {
  def host: String
  def port: Int
  def username: String
  def password: String
  def virtualHost: String
  def connectionTimeout: Int
  def requestedChannelMax: Int
  def requestedFrameMax: Int
  def requestedHeartbeat: Int
}

object RabbitConfig {
  def apply(config: Config): RabbitConfig = new RabbitConfig {
    val host: String             = config.getString("rabbit.host")
    val port: Int                = config.getInt("rabbit.port")
    val username: String         = config.getString("rabbit.username")
    val password: String         = config.getString("rabbit.password")
    val virtualHost: String      = config.getString("rabbit.virtual-host")
    val connectionTimeout: Int   = config.getInt("rabbit.connection-timeout")
    val requestedChannelMax: Int = config.getInt("rabbit.requested-channel-max")
    val requestedFrameMax: Int   = config.getInt("rabbit.requested-frame-max")
    val requestedHeartbeat: Int  = config.getInt("rabbit.requested-heartbeat")
  }
}