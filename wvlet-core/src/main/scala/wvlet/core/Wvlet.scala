/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
