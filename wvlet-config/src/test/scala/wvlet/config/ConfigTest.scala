package wvlet.config

import wvlet.test.WvletSpec

case class MyConfig(id: Int, fullName: String)

/**
  *
  */
class ConfigTest extends WvletSpec {

  val configPaths = Seq("wvlet-config/src/test/resources")

  def loadConfig(env:String) =
    Config.newBuilder(env = env, configPaths = configPaths)
    .registerFromYaml[MyConfig]("myconfig.yml")
    .build


  "Config" should {
    "map yaml file into a case class" in {
      val config = loadConfig("default")

      val c1 = config.of[MyConfig]
      c1.id shouldBe 1
      c1.fullName shouldBe "default-config"
    }

    "read different env config" in {
      val config = loadConfig("staging")

      val c = config.of[MyConfig]
      c.id shouldBe 2
      c.fullName shouldBe "staging-config"
    }

    "allow override" in {
      val config = Config.newBuilder(env = "staging", configPaths = configPaths)
                   .registerFromYaml[MyConfig]("myconfig.yml")
                   .register[MyConfig](MyConfig(10, "hello"))
                   .build

      val c = config.of[MyConfig]
      c.id shouldBe 10
      c.fullName shouldBe "hello"
    }

    "create a new config based on existing one" in {
      val config = Config.newBuilder(env="default", configPaths = configPaths)
                   .registerFromYaml[MyConfig]("myconfig.yml")
                   .build

      val config2 = Config.newBuilder(env="production", configPaths = configPaths)
                    .addAll(config)
                    .build

      val c2 = config2.of[MyConfig]
      c2.id shouldBe 1
      c2.fullName shouldBe "default-config"
    }
  }
}
