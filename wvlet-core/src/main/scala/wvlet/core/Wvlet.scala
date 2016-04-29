package wvlet.core

import wvlet.core.WvletOps.SeqOp
import wvlet.core.tablet.{Tablet, TabletReader, TabletWriter}

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
  * @tparam A
  */
trait Input[A] {
  def write(record: A, output: TabletWriter)
}

/**
  * Tablet -> A converter
  *
  * @tparam A
  */
trait Output[A] {
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

  def json: Output[String] = null
  def tsv: Output[String] = null

}

