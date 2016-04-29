package wvlet.core.rx

/**
  * Source provides a given number of object of type A upon
  * a request from Stream
  * @tparam A
  */
trait Source[A] {
  def run(n: Long)
}

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


/**
  * Flow is an interface to handle streaming objects
  * @tparam A
  */
trait Flow[A] {
  def onStart: Unit
  def onComplete: Unit

  def onError(e: Throwable): Unit
  def onNext(x: A): Unit
}


/**
  * Operation DAG
  *
  *   op = MapOp(SeqOp(seq), f:A=>B)
  *
  * Stream:
  *
  *   op.flow(f1: new Flow(B => action)) : Stream
  *
  *   MapOp: addFlow(f1: Flow[B])
  *   SeqOp: addFlow(f2: new MapFlow[A](MapOp(f:A=>B))
  *          s.setSource(source:Source[A](seq, s2))
  *
  *
  * Data flow:
  *    p --onNext(a)-> s2: A=>B --onNext(b)--> s1: B=> action
  *     ^
  *     |
  *      ---------Source.run(n)----<-- Stream.run(n)
  *
  * Stream Processing Chain
  *
  *   Stream.run(n) // Read n data
  *    - Source.run(n)
  *
  *   Source(Seq[A]) -> f2.onNext(A) -> f1.onNext(f(A):B)
  *
  *
  * Switching Record/Block-wise processing:
  *
  *
  */
