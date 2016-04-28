package wvlet.test

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, ShouldMatchers, WordSpec}
import xerial.core.log.Logger

/**
  *
  */
trait WvletSpec extends WordSpec with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with Logger {

}
