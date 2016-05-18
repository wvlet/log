package wvlet.config

import wvlet.test.WvletSpec


case class MyConfig(id:Int, name:String)

/**
  *
  */
class ConfigServiceTest extends WvletSpec {

  "ConfigService" should {
    "map yaml file into a case class" in {
      val config = new ConfigServiceProvider {}

      val c1 = config.configService.getConfigOf[MyConfig]("wvlet-config/src/test/resources/myconfig.yml", "default")
      c1.id shouldBe 1
      c1.name shouldBe "default-config"

      val c2 = config.configService.getConfigOf[MyConfig]("wvlet-config/src/test/resources/myconfig.yml", "staging")
      c2.id shouldBe 2
      c2.name shouldBe "staging-config"
    }

  }
}
