package wvlet.core

import wvlet.core.WvletOps.SeqOp
import wvlet.core.tablet.{Tablet, TabletReader, TabletWriter}
import wvlet.core.rx.ReactiveStream._
import wvlet.core.rx.{SeqSubscriber, SubscriberBridge}

import scala.reflect.ClassTag

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context: Context): Tablet
}

/**
  * A -> Tablet -> A converter
  *
  * @tparam A
  */
trait Input[A] {
  def write(record: A, output: TabletWriter)
}

trait Output[A] {
  def read(input:TabletReader) : A
}

/**
  * Tablet -> Out type
  *
  * @tparam Out
  */
trait WvletOutput[Out] {

}

import WvletOps._

object Wvlet {

  def create[A: ClassTag](seq: Seq[A]) = SeqOp(seq)

  def json: WvletOutput[String] = null
  def tsv: WvletOutput[String] = null

}

