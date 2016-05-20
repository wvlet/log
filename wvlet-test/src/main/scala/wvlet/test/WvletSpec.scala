package wvlet.test

import org.scalatest._
import wvlet.log.{LogSupport, Logger}
import scala.language.implicitConversions
/**
  *
  */
trait WvletSpec extends WordSpec
  with ShouldMatchers
  with GivenWhenThen
  with BeforeAndAfter
  with BeforeAndAfterAll
  with LogSupport {

  implicit def toTag(s:String) = Tag(s)
}
