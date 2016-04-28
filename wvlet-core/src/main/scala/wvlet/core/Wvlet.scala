package wvlet.core

import wvlet.core.tablet.{Tablet, TabletWriter}

import scala.reflect.ClassTag

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context:Context) : Tablet
}


/**
  * In -> Tablet converter
  * @tparam In
  */
trait WvletInput[In] {
  def |[Out](next:WvletOutput[Out]) : Wvlet[In, Out] = null
  def write(record:In, output:TabletWriter)
}


trait WvletSeq[Input] {
  def mkString(delimiter:String = "") : String

}

trait Observer[A] {
  def onComplete: Unit
  def onFailure(failure: Throwable): Unit
  def onNext(elem: A): Unit
}

trait Producer {
  def request(n:Long)
}

trait Subscription {
  def isSubscribed : Boolean
  def request(n:Long)
  def unsubscribe: Unit
}

trait Subscriber[A] extends Observer[A] with Subscription {
  def onStart : Unit
  def request(n:Long) : Unit
  def setProducer(p:Producer) : Unit
}

trait Wvlet[In, Out] extends WvletInput[Out] {
  def apply(in:Seq[In]) : WvletSeq[Out]

  def subscribe(subscriber:Subscriber[Out])

}


/**
  * Tablet -> Out type
  * @tparam Out
  */
trait WvletOutput[Out] {

}



object Wvlet {

  def create[A : ClassTag](seq:Seq[A]) : WvletInput[A]  = null

  def tabletOf[A : ClassTag]  : WvletInput[A] = {
    null
  }

  def json : WvletOutput[String] = null
  def tsv : WvletOutput[String] = null


}

