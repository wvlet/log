package wvlet.core.rx

import xerial.core.log.Logger
import ReactiveStream._
import wvlet.core.Output
import wvlet.obj.ObjectInput

/**
  * Operation tree:
  *
  *   op = MapOp(SeqOp(seq), f:A=>B)
  *
  * Subscription:
  *
  *   op.subscribe(s1: new Subscriber(B => action)) : Subscription
  *
  *   MapOp: addSubscriber(s1: Subscriber[B])
  *   SeqOp: addSubscriber(s2: new MapOpSubscriber[A](MapOp(f:A=>B))
  *          s1.setPublisher(p:Publisher[A](seq, s2))
  *
  *
  * Flow:
  *    p --onNext(a)-> s2: A=>B --onNext(b)--> s1: B=> action
  *     ^
  *     |
  *      ---------publisher.publish(n)----<-- s1.read(n)
  *
  * Stream Processing Chain
  *
  *   s1.read(n) // Read n data
  *    - p.publish(n)
  *
  *
  *   Publisher(Seq[A]) -> s2.onNext(A) -> s1.onNext(f(A):B)
  *
  *
  *   
  *
  *
  * Switching Record/Block-wise processing
  *
  *
  */
object Subscriber {

}


abstract class SubscriberBridge[In, Out](subscriber: Option[Subscriber[Out]]) extends Subscriber[In] with Flow with Logger {
  private var publisher: Option[Publisher[Out]] = None
  private var running: Boolean = true

  def this() = this(None)
  def this(s:Subscriber[Out]) = this(Some(s))

  override def onComplete = subscriber.map(_.onComplete)
  override def onFailure(failure: Throwable) = subscriber.map(_.onFailure(failure))

//  override def isRunning = running
//  override def stop {
//    running = false
//  }
//

  override def onStart {
    info(s"onStart")
    // do nothing by default
  }

  override def read(n: Long) {
    info(s"request, producer: ${producer}")
    if (n < 0) {
      throw new IllegalArgumentException(s"The number of request cannot be negative: ${ n }")
    }
    publisher.map(_.publish(n))
  }

  def setPublisher(p: Publisher[Out]) {
    publisher = Some(p)
    subscriber.map(s => p.setSubscriber(s))
  }

  protected def guard[U](body: => U) {
    try {
      body
    }
    catch {
      case e: Throwable => subscriber.map(_.onFailure(e))
    }
  }
}


class SeqProducer[A](input:Seq[A]) extends Publisher[A] {

  private var subscriber : Option[Subscriber[A]] = None

  override def setSubscriber(s: Subscriber[A]) {
    subscriber = Some(s)
  }

  override def publish(n: Long): Unit = {
    // TODO back-pressure support
    for(x <- input) {
      subscriber.map(_.onNext(x))
    }
    subscriber.map(_.onComplete)
  }
}

class SeqSubscriber[A](seq:Seq[A], s:Subscriber[A]) extends SubscriberBridge[Unit, A](s) {

  override def onStart: Unit = {
    if(seq.isEmpty) {
      onComplete
    }
    else {
      setPublisher(new SeqProducer(seq))
    }
  }

  override def onNext(elem: Unit): Unit = {
    // Do nothing since this operation has no input
  }
}

class MapOpSubscriber[A, B](f:A=>B, s:Subscriber[B]) extends SubscriberBridge[A, B](s) {
  def onNext(elem:A) {
    s.onNext(f(elem))
  }
}

class FilterOpSubscriber[A](cond:A => Boolean, s:Subscriber[A]) extends SubscriberBridge[A, A](s) {
  override def onNext(elem: A) {
    if(cond(elem)) {
      s.onNext(elem)
    }
  }
}


class ConvertOpSubscriber[A, B](out:Output[B], s:Subscriber[B]) extends SubscriberBridge[A, B](s) {
  val input = new ObjectInput[A]

  override def onStart: Unit = {
    super.onStart

    out.tabletWriter
  }

  override def onNext(elem: A): Unit = {
    input.write(elem, out.tabletWriter)
  }
}