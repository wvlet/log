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

import java.io.FileOutputStream
import java.util.Properties

import wvlet.log.io.IOUtil
import wvlet.obj.tag._
import wvlet.test.WvletSpec

trait AppScope
trait SessionScope

case class SampleConfig(id: Int, fullName: String)

/**
  *
  */
class ConfigTest extends WvletSpec {

  val configPaths = Seq("wvlet-config/src/test/resources")

  def loadConfig(env: String) =
    Config(env = env, configPaths = configPaths)
    .registerFromYaml[SampleConfig]("myconfig.yml")

  "Config" should {
    "map yaml file into a case class" in {
      val config = loadConfig("default")

      val c1 = config.of[SampleConfig]
      c1.id shouldBe 1
      c1.fullName shouldBe "default-config"
    }

    "read different env config" in {
      val config = loadConfig("staging")

      val c = config.of[SampleConfig]
      c.id shouldBe 2
      c.fullName shouldBe "staging-config"
    }

    "allow override" in {
      val config = Config(env = "staging", configPaths = configPaths)
                   .registerFromYaml[SampleConfig]("myconfig.yml")
                   .register[SampleConfig](SampleConfig(10, "hello"))

      val c = config.of[SampleConfig]
      c.id shouldBe 10
      c.fullName shouldBe "hello"
    }

    "create a new config based on existing one" in {
      val config = Config(env = "default", configPaths = configPaths)
                   .registerFromYaml[SampleConfig]("myconfig.yml")

      val config2 = Config(env = "production", configPaths = configPaths) ++ config

      val c2 = config2.of[SampleConfig]
      c2.id shouldBe 1
      c2.fullName shouldBe "default-config"
    }

    "read tagged type" taggedAs ("tag") in {
      val config = Config(env = "default", configPaths = configPaths)
                   .registerFromYaml[SampleConfig @@ AppScope]("myconfig.yml")
                   .register[SampleConfig @@ SessionScope](SampleConfig(2, "session").asInstanceOf[SampleConfig @@ SessionScope])

      val c = config.of[SampleConfig @@ AppScope]
      c shouldBe SampleConfig(1, "default-config")

      val s = config.of[SampleConfig @@ SessionScope]
      s shouldBe SampleConfig(2, "session")
    }

    "override values with properties" taggedAs ("props") in {
      val p = new Properties
      p.setProperty("sample.id", "10")
      p.setProperty("sample@appscope.id", "2")
      p.setProperty("sample@appscope.full_name", "hellohello")

      val c1 = Config(env = "default", configPaths = configPaths)
               .register[SampleConfig](SampleConfig(1, "hello"))
               .register[SampleConfig @@ AppScope](SampleConfig(1, "hellohello").asInstanceOf[SampleConfig @@ AppScope])
               .overrideWithProperties(p)

      class ConfigSpec(config:Config) {
        val c = config.of[SampleConfig]
        c shouldBe SampleConfig(10, "hello")

        val c2 = config.of[SampleConfig @@ AppScope]
        c2 shouldBe SampleConfig(2, "hellohello")
      }

      new ConfigSpec(c1)

      // Using properties files
      IOUtil.withTempFile("config", dir = "target") { file =>
        val f = new FileOutputStream(file)
        p.store(f, "config prop")
        f.close()

        val c2 = Config(env = "default", configPaths = Seq("target"))
                 .register[SampleConfig](SampleConfig(1, "hello"))
                 .register[SampleConfig @@ AppScope](SampleConfig(1, "hellohello"))
                 .overrideWithPropertiesFile(file.getName)

        new ConfigSpec(c2)
      }
    }
  }
}
