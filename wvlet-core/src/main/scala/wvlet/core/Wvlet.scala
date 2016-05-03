package wvlet.core

import wvlet.core.WvletOps.SeqOp
import wvlet.core.tablet._

import scala.reflect.ClassTag

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context: Context): Tablet
}

/**
  * A -> TabletRecord
  *
  */
trait Input {
  def write(record: Any): Record
}

/**
  * Tablet -> A converter
  *
  * @tparam A
  */
trait Output[A] {
  //def inputCls: Class[_]
  def tabletPrinter : TabletPrinter
}

/**
  * Tablet -> Out type
  *
  * @tparam Out
  */
trait WvletOutput[Out] {

}

object Wvlet {

  def create[A: ClassTag](seq: Seq[A]) = SeqOp(seq)

  def toJSON[A](implicit ev: ClassTag[A]) = TabletOutput(ev.runtimeClass, JSONTabletPrinter)
  def toTSV[A](implicit ev: ClassTag[A]) = TabletOutput(ev.runtimeClass, TSVTabletPrinter)
  def toCSV[A](implicit ev: ClassTag[A]) = TabletOutput(ev.runtimeClass, CSVTabletPrinter)

  def fromJSON[A](implicit ev: ClassTag[A]) {}
}

case class TabletOutput[A](inputCls: Class[A], tabletPrinter: TabletPrinter) extends Output[String]
