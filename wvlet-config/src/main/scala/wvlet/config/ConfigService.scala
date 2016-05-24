package wvlet.config

import wvlet.log.LogSupport

import scala.reflect.ClassTag

object Config {
  private[config] case class ConfigKey(cls: Class[_], env: String)

  def newBuilder: ConfigBuilder = new ConfigBuilderImpl
}

trait Config {
  def of[ConfigType](env: String)(implicit ev:ClassTag[ConfigType]): ConfigType
  def of[ConfigType](env: String, default: ConfigType)(implicit ev:ClassTag[ConfigType]): ConfigType
}

trait ConfigProvider {
  def config: Config
}

import wvlet.config.Config._

class ConfigImpl(holder: Map[ConfigKey, AnyRef]) extends Config with LogSupport {
  def of[ConfigType](env: String)(implicit ev:ClassTag[ConfigType]): ConfigType = {
    val cls = ev.runtimeClass
    holder.get(ConfigKey(cls, env)) match {
      case Some(x) => x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"Config[${cls.getName}] for env:${env} is not found")
    }
  }

  def of[ConfigType](env: String, default: ConfigType)(implicit ev:ClassTag[ConfigType]): ConfigType = {
    val cls = ev.runtimeClass
    holder.getOrElse(ConfigKey(cls, env), default).asInstanceOf[ConfigType]
  }
}

trait ConfigBuilder {
  def build: Config
  def registerFromYaml[ConfigType: ClassTag](env: String, configFilePath: String) : ConfigBuilder
  def register[ConfigType: ClassTag](env: String, config: ConfigType) : ConfigBuilder
}

class ConfigBuilderImpl extends ConfigBuilder {

  private var configHolder = Map.empty[ConfigKey, AnyRef]

  def build: Config = new ConfigImpl(configHolder)

  def register[ConfigType](env: String, config: ConfigType)(implicit ev:ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    configHolder += ConfigKey(cls, env) -> config.asInstanceOf[AnyRef]
    this
  }

  def registerFromYaml[ConfigType](env: String, configFilePath: String)(implicit ev:ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    configHolder += ConfigKey(cls, env) -> YamlReader.load[ConfigType](configFilePath, env).asInstanceOf[AnyRef]
    this
  }
}
