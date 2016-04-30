package wvlet.core.rx


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
