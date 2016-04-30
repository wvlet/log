package wvlet.core.rx

import wvlet.core.{WvSeq, WvletOp}
import wvlet.core.WvletOps.{ConvertOp, SeqOp}

/**
  *
  */
abstract class StreamBase(sources:Seq[Source[_]]) extends Stream {

  override def start {
    for(s <- sources.par) {
      s.run(Long.MaxValue)
    }
  }
  override def stop: Unit {

  }
  override def run(n: Long): Unit = {
    for(s <- sources.par) {
      s.run(n)
    }
  }

  override def close: Unit = {

  }
}


object Stream {


  private def newFlow[A](op:WvletOp[A]) : Flow[A] = {
    
  }


  def build[A](op:WvSeq[A], s: Flow[A]) : Stream = {
    val s = op match {
      case SeqOp(seq) =>
        new SeqFlow(seq, s)
      case ConvertOp(prev, out) =>
        new ConvertFlow(out, s)
    }
    new StreamBase()
    s.asInstanceOf[Stream]
  }

}