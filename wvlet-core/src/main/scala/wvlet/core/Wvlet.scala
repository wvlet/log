package wvlet.core

import wvlet.core.WvletOps.{ConvertOp, SeqOp}
import wvlet.core.rx.Flow
import wvlet.core.tablet.{JSONTabletWriter, Tablet, TabletReader, TabletWriter}

import scala.reflect.ClassTag

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context: Context): Tablet
}

/**
  * A -> Tablet
  *
  */
trait Input {
  def write(record: Any, output: TabletWriter, flow:Flow[String])
}

/**
  * Tablet -> A converter
  *
  * @tparam A
  */
trait Output[A] {
  def inputCls : Class[_]
  def tabletWriter : TabletWriter
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

  def json[A](implicit ev:ClassTag[A]) = Converter(ev.runtimeClass, JSONTabletWriter)
  //def tsv: Output[String] = null

}

case class Converter[A](inputCls:Class[A], tabletWriter: TabletWriter) extends Output[String]