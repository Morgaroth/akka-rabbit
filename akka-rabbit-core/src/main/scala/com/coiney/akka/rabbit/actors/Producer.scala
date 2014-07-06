package com.coiney.akka.rabbit.actors

import akka.actor.Props


object Producer {
  def apply(): Producer = new Producer()

  def props(): Props = Props(Producer())
}


class Producer extends ChannelKeeper {

}
