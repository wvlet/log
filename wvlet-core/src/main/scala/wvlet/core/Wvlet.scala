package wvlet.core

import wvlet.core.tablet.Tablet

import scala.reflect.ClassTag

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context:Context) : Tablet
}


trait Input {



}

trait Output[A] {
  def onCompleted
  def onError(cause:Throwable)
  def onNext(e:A)
}

trait Producer[A]


trait WvletInput {

  def process(context:Context, input:Input, output:Output) {
  }

}


trait Wvlet[In, Out] {

  def |[R](next:Wvlet[Out, R]) : Wvlet[In, R]

  def apply(in:Seq[In]) : WvletSeq[Out]


}

trait WvletSeq[Input] {
  def mkString(delimiter:String = "") : String
}


object Wvlet {

  def tabletOf[A : ClassTag]  : Wvlet[A, Tablet] = {
    null
  }

  def json : Wvlet[Tablet, String] = null
  def tsv : Wvlet[Tablet, String] = null


}


class ObjectWvlet[A] extends Wvlet[A, Tablet] {
  override def |[R](next: Wvlet[Tablet, R]): Wvlet[A, R] = {


  }
  override def apply(in: Seq[A]): WvletSeq[Tablet] = ???
}