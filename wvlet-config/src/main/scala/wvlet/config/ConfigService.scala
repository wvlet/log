package wvlet.config

import java.io.File

import scala.reflect.ClassTag

/**
  *
  */
trait ConfigService {
  def getConfigOf[ConfigType : ClassTag](path:String, env:String) : ConfigType
}

trait ConfigServiceProvider {
  val configService : ConfigService = new YamlConfigService
}

class YamlConfigService(basePath:String = "") extends ConfigService {
  require(basePath != null, "basePath is null")

  override def getConfigOf[ConfigType: ClassTag](path: String, env: String): ConfigType = {
    val configFilePath =
      if(basePath.isEmpty)
        new File(path)
      else
        new File(basePath, path)

    YamlReader.load[ConfigType](configFilePath.getPath, env)
  }
}
