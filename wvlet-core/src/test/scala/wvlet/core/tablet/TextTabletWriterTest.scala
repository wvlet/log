package wvlet.core.tablet

import wvlet.test.WvletSpec

object TextTabletWriterTest {
  case class Person(id:Int, name:String)
}

import TextTabletWriterTest._
/**
  *
  */
class TextTabletWriterTest extends WvletSpec {

  import wvlet.core._
  import Wvlet._

  "TextTabletWriter" should {

    "output object in JSON array format" in {

      val seq = Seq(Person(1, "leo"), Person(2, "yui"))


      val w  = create(seq) | json

      w.mkString("\n")

    }

  }
}
