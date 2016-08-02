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
