package wvlet.core

import wvlet.core.tablet.Tablet

trait Context

trait Router {
  /**
    * Find a destination tablet for the given context
    */
  def findTablet(context:Context) : Tablet
}

  case class Wvlet(name: String, router: Router)

trait Input {



}

trait Output[A] {

  def onCompleted
  def onError(cause:Throwable)
  def onNext(e:A)
}

trait WvletInput {

  def process(context:Context, input:Input, output:Output) {
  }

}
