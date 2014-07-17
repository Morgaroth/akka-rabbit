package com.coiney.akka.rabbit.actors

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

trait RabbitConnectionProvider {
  this: RabbitConfiguration =>

  protected val connectionFactory: ConnectionFactory = {
    val connectionFactory = new ConnectionFactory()
    connectionFactory.setHost(settings.host)
    connectionFactory.setPort(settings.port)
    connectionFactory.setUsername(settings.username)
    connectionFactory.setPassword(settings.password)
    connectionFactory.setVirtualHost(settings.virtualHost)
    connectionFactory.setConnectionTimeout(settings.connectionTimeout)
    connectionFactory.setRequestedChannelMax(settings.requestedChannelMax)
    connectionFactory.setRequestedFrameMax(settings.requestedFrameMax)
    connectionFactory.setRequestedHeartbeat(settings.requestedHeartbeat)
    connectionFactory
  }

  def createConnection(): Connection = connectionFactory.newConnection()

  // The URI of the connection
  def connectionUri: String =
    s"amqp://${settings.username}:${settings.password}@${settings.host}:${settings.port}/${settings.virtualHost}"

  // The URI of the connection, stripped from the credentials
  def safeConnectionUri: String =
    s"amqp://${settings.username}@${settings.host}:${settings.port}/${settings.virtualHost}"

}
