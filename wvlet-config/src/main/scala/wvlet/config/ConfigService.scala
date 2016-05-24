package wvlet.config

import wvlet.log.LogSupport

import scala.reflect.ClassTag

object Config {
  private[config] case class ConfigHolder(env: String, cls:Class[_], value:Any)

  def newBuilder: ConfigBuilder = new ConfigBuilderImpl
  def newBuilder(base:Config): ConfigBuilder = {
    new ConfigBuilderImpl().add(base)
  }
}

import Config._

trait Config extends Iterable[ConfigHolder]{
  def of[ConfigType](env: String)(implicit ev:ClassTag[ConfigType]): ConfigType
  def of[ConfigType](env: String, default: ConfigType)(implicit ev:ClassTag[ConfigType]): ConfigType
}

trait ConfigProvider {
  def config: Config
}

import wvlet.config.Config._

class ConfigImpl(holder: Seq[ConfigHolder]) extends Config with LogSupport {

  private def find[A](env:String, cls:Class[A]) : Option[A] = {
    holder.find(x => x.env == env && x.cls == cls).map{_.value.asInstanceOf[A]}
  }

  def of[ConfigType](env: String)(implicit ev:ClassTag[ConfigType]): ConfigType = {
    val cls = ev.runtimeClass
    find(env, cls) match {
      case Some(x) => x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"Config[${cls.getName}] for env:${env} is not found")
    }
  }

  def of[ConfigType](env: String, default: ConfigType)(implicit ev:ClassTag[ConfigType]): ConfigType = {
    val cls = ev.runtimeClass
    find(env, cls).getOrElse(default).asInstanceOf[ConfigType]
  }

  override def iterator: Iterator[ConfigHolder] = holder.iterator
}

trait ConfigBuilder {
  def build: Config
  def registerFromYaml[ConfigType: ClassTag](env: String, configFilePath: String): ConfigBuilder
  def registerAllFromYaml[ConfigType: ClassTag](configFilePath: String): ConfigBuilder
  def register[ConfigType: ClassTag](env: String, config: ConfigType): ConfigBuilder
  def add(config: Config): ConfigBuilder
}

class ConfigBuilderImpl extends ConfigBuilder {

  private var configHolder = Seq.newBuilder[ConfigHolder]

  def add(config:Config) : ConfigBuilder = {
    configHolder ++= config
    this
  }

  def build: Config = {
    // Override previous occurrences of the same type config
    val b = Map.newBuilder[(Class[_], String), ConfigHolder]
    for(s <- configHolder.result) {
      b += (s.cls, s.env) -> s
    }
    new ConfigImpl(b.result().values.toIndexedSeq)
  }

  def register[ConfigType](env: String, config: ConfigType)(implicit ev:ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    configHolder += ConfigHolder(env, cls, config)
    this
  }

  def registerFromYaml[ConfigType](env: String, configFilePath: String)(implicit ev:ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    configHolder += ConfigHolder(env, cls, YamlReader.load[ConfigType](configFilePath, env))
    this
  }

  def registerAllFromYaml[ConfigType](configFilePath: String)(implicit ev:ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    for((k, v) <- YamlReader.loadMapOf(configFilePath)) {
      val env = k.toString
      configHolder += ConfigHolder(env, cls, v)
    }
    this
  }

}
