package wvlet.core.rx

import wvlet.core.Output
import wvlet.core.WvletOps.{MapOp, SeqOp}
import wvlet.obj.ObjectInput

class SeqSource[A](input: Seq[A], flow: Flow[A]) extends Source[A] {
  private var isStarted = false
  private val cursor    = input.iterator

  override def run(n: Long) {
    if (n < 0) {
      throw new IllegalArgumentException(s"The number of request cannot be negative: ${n}")
    }

    if (!isStarted && n > 0) {
      flow.onStart
    }

    var remaining = n
    while (remaining > 0 && cursor.hasNext) {
      val x = cursor.next()
      remaining -= 1
      flow.onNext(x)
    }

    if (!cursor.hasNext) {
      flow.onComplete
    }
  }
}

abstract class FlowBase[In, Out](flow: Flow[Out]) extends Flow[In] {
  def onStart {
    flow.onStart
  }

  def onComplete {
    flow.onComplete
  }

  def onError(e: Throwable) {
    flow.onError(e)
  }
}

class SeqFlow[A](op: SeqOp[A], flow: Flow[A]) extends FlowBase[Unit, A](flow) {

  override def onStart: Unit = {
    super.onStart
    if (op.seq.isEmpty) {
      onComplete
    }
  }

  override def onNext(x: Unit): Unit = {
    // Do nothing since this operation has no input
  }
}

class MapFlow[A, B](op: MapOp[A, B], flow: Flow[B]) extends FlowBase[A, B](flow) {
  def onNext(x: A) {
    flow.onNext(op.f(x))
  }
}

class FilterFlow[A](cond: A => Boolean, flow: Flow[A]) extends FlowBase[A, A](flow) {
  override def onNext(elem: A) {
    if (cond(elem)) {
      flow.onNext(elem)
    }
  }
}

class ConvertOpSubscriber[A, B](out: Output[B], s: Subscriber[B]) extends SubscriberBridge[A, B](s) {
  val input = new ObjectInput[A]

  override def onStart: Unit = {
    super.onStart

    out.tabletWriter
  }

  override def onNext(elem: A): Unit = {
    input.write(elem, out.tabletWriter)
  }
}