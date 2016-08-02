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
package wvlet.core.rx

import wvlet.core.WvletOps.{ConvertOp, FilterOp, MapOp, SeqOp}
import wvlet.core.tablet.Record
import wvlet.core.{WvSeq, WvletOp}

/**
  * Stream controls the data flow from Source(s).
  * The data objects streams through Flow operations
  */
trait Stream {
  def start
  def run(n:Long)
  def stop
  def close
}

object Stream {

  def build[A](op: WvSeq[A], next: Flow[A]): Stream = {
    val b = new StreamBuilder
    b.newFlow(op, next)
    b.result
  }
}

class StreamBuilder {

  private val sources = Seq.newBuilder[Source[_]]

  def newFlow[A](op: WvletOp[A], next: Flow[A]): Flow[_] = {
    op match {
      case SeqOp(seq) =>
        sources += new SeqSource(seq, next)
        next
      case MapOp(in, f) =>
        newFlow(in, new MapFlow(f, next))
      case FilterOp(in, cond) =>
        newFlow(in, new FilterFlow(cond, next))
      case ConvertOp(in, out) =>
        newFlow(in, new ConvertFlow(out, new RecordPrintFlow(out.tabletPrinter, next.asInstanceOf[Flow[String]])))
    }
  }

  def result: Stream = new SimpleStream(sources.result())
}

/**
  *
  */
class SimpleStream(sources: Seq[Source[_]]) extends Stream {

  override def start {
    for (s <- sources.par) {
      s.run(Long.MaxValue)
    }
  }

  override def stop {
    // TODO
  }

  override def run(n: Long) {
    for (s <- sources.par) {
      s.run(n)
    }
  }

  override def close {

  }
}

