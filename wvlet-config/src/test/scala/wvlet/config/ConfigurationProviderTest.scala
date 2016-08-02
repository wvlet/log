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
package wvlet.config

import wvlet.inject.{Inject, _}
import wvlet.test.WvletSpec

object ConfigurationProviderTest {

  case class ConfigA(id: Int, fullName: String)

  trait MyApp {
    val configA = inject[ConfigA]
  }

}

import wvlet.config.ConfigurationProviderTest._

/**
  *
  */
class ConfigurationProviderTest extends WvletSpec {

  "ConfigurationProvider" should {

    "provide config objects" in {

      val config =
        Config.newBuilder("staging")
        .registerFromYaml[ConfigA]("wvlet-config/src/test/resources/myconfig.yml")
        .build

      val i = new Inject
      config.bindConfigs(i)
      val c = i.newContext

      val myapp = c.build[MyApp]
      myapp.configA shouldBe ConfigA(2, "staging-config")
    }

  }
}
