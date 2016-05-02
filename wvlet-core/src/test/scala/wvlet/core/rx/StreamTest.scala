package wvlet.core.rx

import wvlet.test.WvletSpec

/**
  *
  */
class StreamTest extends WvletSpec {

  "Stream" should {
    "create an operation chain" in {

      import wvlet.core._


      val wv =
        Wvlet
        .create(Seq(1, 2, 3, 4))
        .filter(_ % 2 == 0)  // [2, 4]
        .map(_ * 3) // [6, 12]

      wv.stream(x => println(x))

    }


  }
}
