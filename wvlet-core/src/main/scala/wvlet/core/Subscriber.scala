package wvlet.core

import xerial.core.log.Logger

/**
  *
  */

abstract class SubscriberBase[-In, Out](child: Option[Subscriber[Out]]) extends Subscriber[In] with Logger {
  private var producer: Option[Producer] = None
  private var subscribed: Boolean = true

  def this(child: Subscriber[Out]) = this(Some(child))
  def this() = this(None)

  override def onComplete = child.map(_.onComplete)
  override def onFailure(failure: Throwable) = child.map(_.onFailure(failure))

  override def isSubscribed = subscribed
  override def unsubscribe {
    subscribed = false
  }

  override def onStart: Unit = {
    info(s"onStart")
    // do nothing by default
  }

  override def request(n: Long) {
    info(s"request, producer: ${producer}")
    if (n < 0) {
      throw new IllegalArgumentException(s"The number of request cannot be negative: ${ n }")
    }
    producer.map(_.request(n))
  }

  override def setProducer(p: Producer) {
    producer = Some(p)
    child.map(_.setProducer(p))
  }

  protected def guard[U](body: => U) {
    try {
      body
    }
    catch {
      case e: Throwable => child.map(_.onFailure(e))
    }
  }
}
