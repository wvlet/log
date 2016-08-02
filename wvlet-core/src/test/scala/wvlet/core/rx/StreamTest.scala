/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
