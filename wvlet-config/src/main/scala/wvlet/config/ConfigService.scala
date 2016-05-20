package wvlet.config

import java.io.File

import scala.reflect.ClassTag

/**
  *
  */
trait Config {
  def of[ConfigType : ClassTag](env:String) : ConfigType
}

trait ConfigProvider {
  val config : Config = new YamlConfig
}

trait YamlConfig extends Config {
  val configPath : String

  require(configPath != null, "basePath is null")

  def of[ConfigType: ClassTag](path:String, env: String): ConfigType = {
    val configFilePath =
      if(configPath.isEmpty)
        new File(path)
      else
        new File(configPath, path)

    YamlReader.load[ConfigType](configFilePath.getPath, env)
  }
}
