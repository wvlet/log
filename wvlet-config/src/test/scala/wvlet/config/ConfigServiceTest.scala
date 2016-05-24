package wvlet.config

import wvlet.test.WvletSpec

case class MyConfig(id: Int, fullName: String)

/**
  *
  */
class ConfigServiceTest extends WvletSpec {

  "ConfigService" should {
    "map yaml file into a case class" in {

      val config = Config.newBuilder
                   .registerFromYaml[MyConfig]("default", "wvlet-config/src/test/resources/myconfig.yml")
                   .registerFromYaml[MyConfig]("staging", "wvlet-config/src/test/resources/myconfig.yml")
                   .build

      val c1 = config.of[MyConfig]("default")
      c1.id shouldBe 1
      c1.fullName shouldBe "default-config"

      val c2 = config.of[MyConfig]("staging")
      c2.id shouldBe 2
      c2.fullName shouldBe "staging-config"
    }

  }
}
