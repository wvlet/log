package wvlet.core

import wvlet.core.WvletOps.{ConvertOp, FilterOp, MapOp}
import wvlet.core.rx.ReactiveStream.Subscriber
import wvlet.core.rx.SubscriberBridge

sealed trait WvletOp

/**
  *
  */
object WvletOps {
  case class SeqOp[A](seq: Seq[A]) extends WvSeq[A]
  case class MapOp[A, B](prev: WvSeq[A], f: A => B) extends WvSeq[B]
  case class FilterOp[A](prev: WvSeq[A], cond: A => Boolean) extends WvSeq[A]
  case class ConvertOp[A, R](prev: WvSeq[A], out: WvletOutput[R]) extends WvSeq[R]
}

trait WvSeq[A] extends WvletOp {
  self =>
  def map[B](f: A => B): WvSeq[B] = MapOp(self, f)
  def filter(cond: A => Boolean): WvSeq[A] = FilterOp(self, cond)
  def |[R](out: WvletOutput[R]): WvSeq[R] = ConvertOp(self, out)

  def subscribe(subscriber: Subscriber[A]) {
    subscriber.onStart
    subscriber.read(Long.MaxValue)
  }
  def subscribe[U](handler: A => U) = {
    val s = new SubscriberBridge[A, U]() {
      override def onNext(elem: A): Unit = handler(elem)
    }
    subscribe(s)
  }
}
