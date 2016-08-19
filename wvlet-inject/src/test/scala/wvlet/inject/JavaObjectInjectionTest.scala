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
package wvlet.inject

import org.komamitsu.fluency.Fluency
import wvlet.log.{LogLevel, Logger}
import wvlet.test.WvletSpec

trait FluencyLogging {
  val log = inject[Fluency]
}

trait MetricService {
  val m = inject[FluencyLogging]
}

/**
  *
  */
class JavaObjectInjectionTest extends WvletSpec {
  "Inject" should {
    "detect uninjectable objects" in {
      val i = new Inject
      val s = i.newSession
      intercept[InjectionException] {
        // Fluency has no default constructor. We cannot inject Session to it, so the user needs to bind it explicitly to some instance
        val service = s.build[FluencyLogging]
      }
    }

    "inject Fluency" in {
      val i = new Inject
      i.bind[Fluency].toInstance(Fluency.defaultFluency)
      val s = i.newSession
      val service = s.build[FluencyLogging]
    }

    "inject Fluency in nested scope" in {
      val i = new Inject
      i.bind[Fluency].toInstance(Fluency.defaultFluency)
      val s = i.newSession
      val metricService = s.build[MetricService]
    }

    "compile Fluency using reflection" taggedAs ("reflection") in {
      import scala.reflect.runtime.currentMirror
      import scala.tools.reflect.ToolBox
      val tb = currentMirror.mkToolBox()
      tb.eval(tb.parse("new { val f : org.komamitsu.fluency.Fluency = null }"))
    }
  }
}
