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

    "load all envs in a yaml file" in {
      val config = Config.newBuilder
                   .registerAllFromYaml[MyConfig]("wvlet-config/src/test/resources/myconfig.yml")
                   .build

      val c1 = config.of[MyConfig]("default")
      c1.id shouldBe 1
      c1.fullName shouldBe "default-config"

      val c2 = config.of[MyConfig]("staging")
      c2.id shouldBe 2
      c2.fullName shouldBe "staging-config"
    }

    "override config" in {
      val config = Config.newBuilder
                   .registerAllFromYaml[MyConfig]("wvlet-config/src/test/resources/myconfig.yml")
                   .register[MyConfig]("default", MyConfig(10, "hello"))
                   .build

      val c1 = config.of[MyConfig]("default")
      c1.id shouldBe 10
      c1.fullName shouldBe "hello"

      val c2 = config.of[MyConfig]("staging")
      c2.id shouldBe 2
      c2.fullName shouldBe "staging-config"
    }

    "create a new config based on existing one" in {
      val config = Config.newBuilder
                   .registerAllFromYaml[MyConfig]("wvlet-config/src/test/resources/myconfig.yml")
                   .build
      val config2 = Config.newBuilder(config)
                    .register[MyConfig]("staging", MyConfig(3, "staging-2"))
                    .build

      val c1 = config2.of[MyConfig]("default")
      c1.id shouldBe 1
      c1.fullName shouldBe "default-config"

      val c2 = config2.of[MyConfig]("staging")
      c2.id shouldBe 3
      c2.fullName shouldBe "staging-2"
    }



  }
}
