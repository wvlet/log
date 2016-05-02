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
