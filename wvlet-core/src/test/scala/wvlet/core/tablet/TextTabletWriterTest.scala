package wvlet.core.tablet

import wvlet.test.WvletSpec

object TextTabletWriterTest {
  case class Person(id:Int, name:String, phone:Seq[String])
}


import TextTabletWriterTest._
/**
  *
  */
class TextTabletWriterTest extends WvletSpec {

  import wvlet.core._
  import Wvlet._

  "TextTabletWriter" should {

    val seq = Seq(Person(1, "leo", Seq("xxx-xxxx")), Person(2, "yui", Seq("yyy-yyyy", "zzz-zzzz")))

    "output object in JSON array format" in {
      val w  = create(seq) | toJSON[Person]
      w.stream(info(_))
    }

    "output object in CSV format" in {
      val w  = create(seq) | toCSV[Person]
      w.stream(info(_))
    }

    "output object in TSV format" in {
      val w  = create(seq) | toTSV[Person]
      w.stream(info(_))
    }

  }
}
