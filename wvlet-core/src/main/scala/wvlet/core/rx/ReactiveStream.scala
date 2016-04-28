package wvlet.core.rx

object ReactiveStream {
  trait Publisher[A] {
    def setSubscriber(s: Subscriber[A])

    def publish(n: Long)
  }

  trait Flow {
//    def isRunning
//    def stop: Unit

  }

  /**
    * Subscriber signals Flow.read(n) to receive onNext events.
    *
    * @tparam A
    */
  trait Subscriber[A] {

    def onStart: Unit

    def onComplete: Unit
    def onFailure(failure: Throwable): Unit
    def onNext(elem: A): Unit

    def read(n: Long)
  }

}


