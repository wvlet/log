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
