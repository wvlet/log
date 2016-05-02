package wvlet.core

/**
  * Reactive Stream interfaces:
  *
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
package object rx {

}
