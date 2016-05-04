package wvlet.test

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, ShouldMatchers, WordSpec}
import wvlet.log.Logger

/**
  *
  */
trait WvletSpec extends WordSpec with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with Logger {

}
