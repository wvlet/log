package wvlet.core.rx

import wvlet.core.Output
import wvlet.core.WvletOps.{MapOp, SeqOp}
import wvlet.core.tablet.{ObjectInput, Record, TabletPrinter}

/**
  * Flow is an interface to handle streaming objects
  * @tparam A
  */
trait Flow[A] {
  def onStart: Unit
  def onComplete: Unit

  def onError(e: Throwable): Unit
  def onNext(x: A): Unit
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

class MapFlow[A, B](f:A=>B, flow: Flow[B]) extends FlowBase[A, B](flow) {
  def onNext(x: A) {
    flow.onNext(f(x))
  }
}

class FilterFlow[A](cond: A => Boolean, flow: Flow[A]) extends FlowBase[A, A](flow) {
  override def onNext(elem: A) {
    if (cond(elem)) {
      flow.onNext(elem)
    }
  }
}

class ConvertFlow[A, B](out: Output[B], flow: Flow[Record]) extends FlowBase[A, Record](flow) {
  val input = new ObjectInput()

  override def onNext(elem: A): Unit = {
    val record = input.write(elem)
    flow.onNext(record)
  }
}

class RecordPrintFlow(printer:TabletPrinter, flow:Flow[String]) extends FlowBase[Record, String](flow) {

  override def onNext(x: Record): Unit = {
    val s = printer.write(x)
    flow.onNext(s)
  }
}