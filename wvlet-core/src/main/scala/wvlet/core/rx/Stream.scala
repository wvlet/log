package wvlet.core.rx

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
