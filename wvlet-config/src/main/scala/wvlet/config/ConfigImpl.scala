package wvlet.config

import wvlet.config.Config._
import wvlet.log.LogSupport

import scala.reflect.ClassTag

class ConfigImpl(holder: Seq[ConfigHolder]) extends Config with LogSupport {

  private def find[A](env: String, cls: Class[A]): Option[A] = {
    holder.find(x => x.env == env && x.cls == cls).map {_.value.asInstanceOf[A]}
  }

  def of[ConfigType](env: String)(implicit ev: ClassTag[ConfigType]): ConfigType = {
    val cls = ev.runtimeClass
    find(env, cls) match {
      case Some(x) => x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"Config[${cls.getName}] for env:${env} is not found")
    }
  }

  def of[ConfigType](env: String, default: ConfigType)(implicit ev: ClassTag[ConfigType]): ConfigType = {
    val cls = ev.runtimeClass
    find(env, cls).getOrElse(default).asInstanceOf[ConfigType]
  }

  override def iterator: Iterator[ConfigHolder] = holder.iterator
}

class ConfigBuilderImpl extends ConfigBuilder with LogSupport {

  private var configHolder = Seq.newBuilder[ConfigHolder]

  def addAll(config: Config): ConfigBuilder = {
    configHolder ++= config
    this
  }

  def add(config: ConfigHolder): ConfigBuilder = {
    configHolder += config
    this
  }

  def build: Config = {
    // Override previous occurrences of the same type config
    val b = Map.newBuilder[(Class[_], String), ConfigHolder]
    for (s <- configHolder.result) {
      b += (s.cls, s.env) -> s
    }
    new ConfigImpl(b.result().values.toIndexedSeq)
  }

  def register[ConfigType](env: String, config: ConfigType)(implicit ev: ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    configHolder += ConfigHolder(env, cls, config)
    this
  }

  def registerFromYaml[ConfigType](configFilePath: String, env:String, defaultEnv:String)(implicit ev: ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    info(s"Loading configuration in ${configFilePath}, env:${env}")
    val m = YamlReader.loadMapOf[ConfigType](configFilePath)
    val config = m.get(env) match {
      case Some(x) => x
      case None =>
        // Load default
        warn(s"Configuration for ${env} is not found in ${configFilePath}. Load ${defaultEnv} configuration instead")
        m.getOrElse(
          defaultEnv,
          {
            val m = s"No congiguration for ${defaultEnv} is found in ${configFilePath}"
            error(m)
            throw new IllegalArgumentException(m)
          }
        )
    }
    configHolder += ConfigHolder(env, cls, config)
    this
  }

  def registerAllFromYaml[ConfigType](configFilePath: String)(implicit ev: ClassTag[ConfigType]): ConfigBuilder = {
    val cls = ev.runtimeClass
    info(s"Loading configuration in ${configFilePath}")
    for ((k, v) <- YamlReader.loadMapOf(configFilePath)) {
      val env = k.toString
      configHolder += ConfigHolder(env, cls, v)
    }
    this
  }

}
