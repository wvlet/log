package wvlet.core

import wvlet.core.WvletOps.{ConvertOp, FilterOp, MapOp, MkStringOp}
import wvlet.core.rx.ReactiveStream.Subscriber
import wvlet.core.rx.{ConvertOpSubscriber, SeqSubscriber, SubscriberBridge}

trait WvletOp

/**
  *
  */
object WvletOps {

  case class SeqOp[A](seq: Seq[A]) extends WvSeq[A]
  case class MapOp[A, B](prev: WvSeq[A], f: A => B) extends WvSeq[B]
  case class FilterOp[A](prev: WvSeq[A], cond: A => Boolean) extends WvSeq[A]
  case class ConvertOp[A, R](prev: WvSeq[A], out: Output[R]) extends WvSeq[R]
  case class MkStringOp[A](prev: WvSeq[A], separator:String) extends WvSingle[String]

  def lift[A](op:WvSeq[A], s:Subscriber[A]) : Subscriber[A] = {
    val s = op match {
      case SeqOp(seq) =>
        new SeqSubscriber(seq, s)
      case ConvertOp(prev, out) =>
        new ConvertOpSubscriber(out, s)
    }
    s.asInstanceOf[Subscriber[A]]
  }

}

trait WvSingle[A] extends WvletOp {

}

trait WvSeq[A] extends WvletOp {
  self =>
  def map[B](f: A => B): WvSeq[B] = MapOp(self, f)
  def filter(cond: A => Boolean): WvSeq[A] = FilterOp(self, cond)
  def |[R](out: Output[R]): WvSeq[R] = ConvertOp(self, out)
  def mkString(sepaarator:String) : WvSingle[String] = MkStringOp(self, sepaarator)

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
