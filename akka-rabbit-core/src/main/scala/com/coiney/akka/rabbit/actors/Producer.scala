package com.coiney.akka.rabbit.actors

import akka.actor.Props


object Producer {
  def props(): Props = Props(classOf[Producer])
}


class Producer extends ChannelKeeper {

}
