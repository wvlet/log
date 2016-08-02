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


/**
  * Source provides a given number of object of type A upon
  * a request from Stream
  * @tparam A
  */
trait Source[A] {
  def run(n: Long)
}

class SeqSource[A](input: Seq[A], flow: Flow[A]) extends Source[A] {
  private var isStarted = false
  private val cursor    = input.iterator

  override def run(n: Long) {
    if (n < 0) {
      throw new IllegalArgumentException(s"The number of request cannot be negative: ${n}")
    }

    if (!isStarted) {
      isStarted = true
      flow.onStart
    }

    var remaining = n
    while (remaining > 0 && cursor.hasNext) {
      val x = cursor.next()
      remaining -= 1
      flow.onNext(x)
    }

    if (!cursor.hasNext) {
      flow.onComplete
    }
  }
}
