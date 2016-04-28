package wvlet.core

import wvlet.core.tablet.{Tablet, TabletWriter}
import wvlet.core.rx.ReactiveStream._
import wvlet.core.rx.SubscriberBridge

import scala.reflect.ClassTag

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context: Context): Tablet
}

/**
  * In -> Tablet converter
  *
  * @tparam In
  */
trait WvletInput[In] {
  def |[Out](next: WvletOutput[Out]): Wvlet[In, Out] = null
  def write(record: In, output: TabletWriter)
}

trait WvletSeq[Input] {
  def mkString(delimiter: String = ""): String

}

trait Wvlet[In, Out] extends WvletInput[Out] {
  def apply(in: Seq[In]): WvletSeq[Out]

  def subscribe(subscriber: Subscriber[Out]) {
    subscriber.onStart
    subscriber.read(Long.MaxValue)
  }
  def subscribe[U](handler: Out => U) = {
    val s = new SubscriberBridge[Out, U]() {
      override def onNext(elem: Out): Unit = handler(elem)
    }
    subscribe(s)
  }
}

/**
  * Tablet -> Out type
  *
  * @tparam Out
  */
trait WvletOutput[Out] {

}

object Wvlet {

  def create[A: ClassTag](seq: Seq[A]): WvletInput[A] = null

  def tabletOf[A: ClassTag]: WvletInput[A] = {
    null
  }

  def json: WvletOutput[String] = null
  def tsv: WvletOutput[String] = null

}

