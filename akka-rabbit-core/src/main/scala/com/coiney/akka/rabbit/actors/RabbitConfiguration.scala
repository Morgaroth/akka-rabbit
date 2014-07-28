package com.coiney.akka.rabbit.actors

import com.coiney.akka.rabbit.RabbitSystem


trait RabbitConfiguration {
  protected def settings: RabbitSystem.Settings
}
