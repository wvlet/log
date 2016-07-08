package wvlet.config

import scala.reflect.ClassTag

object Config {
  private[config] case class ConfigHolder(env: String, cls: Class[_], value: Any)

  def newBuilder: ConfigBuilder = new ConfigBuilderImpl
  def newBuilder(base: Config): ConfigBuilder = {
    new ConfigBuilderImpl().addAll(base)
  }
}

import wvlet.config.Config._

trait Config extends Iterable[ConfigHolder] {
  def of[ConfigType](env: String)(implicit ev: ClassTag[ConfigType]): ConfigType
  def of[ConfigType](env: String, default: ConfigType)(implicit ev: ClassTag[ConfigType]): ConfigType
}

trait ConfigProvider {
  def config: Config
}

trait ConfigBuilder {
  def build: Config
  def registerFromYaml[ConfigType: ClassTag](env: String, configFilePath: String): ConfigBuilder
//  def registerFromYaml[ConfigType: ClassTag](env: String, configFilePath: String, defaultEnv:String): ConfigBuilder
  def registerAllFromYaml[ConfigType: ClassTag](configFilePath: String): ConfigBuilder
  def register[ConfigType: ClassTag](env: String, config: ConfigType): ConfigBuilder
  def addAll(config: Config): ConfigBuilder
  def add(config:ConfigHolder) : ConfigBuilder
}

