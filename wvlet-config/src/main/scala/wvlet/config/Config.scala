package wvlet.config

import java.io.{File, FileNotFoundException}

import wvlet.inject.Inject
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

object Config {
  private def defaultConfigPath = Seq(
    ".", // current directory
    sys.props.getOrElse("prog.home", "") // program home
  )

  def newBuilder(env: String, configPaths: Seq[String]=defaultConfigPath): ConfigBuilder
  = new ConfigBuilderImpl(Environment(env), cleanupConfigPaths(configPaths))

  def newBuilder(env: Environment, configPaths: Seq[String]): ConfigBuilder =
    new ConfigBuilderImpl(env, cleanupConfigPaths(configPaths))

  private def cleanupConfigPaths(paths:Seq[String]) = {
    val b = Seq.newBuilder[String]
    for(p <- paths) {
      if(!p.isEmpty) {
        b += p
      }
    }
    val result = b.result()
    if(result.isEmpty) {
      Seq(".") // current directory
    }
    else {
      result
    }
  }

}

trait Config extends Iterable[ConfigHolder] {
  def of[ConfigType](implicit tag: ru.TypeTag[ConfigType]): ConfigType

  def bindConfigs(i: Inject)
}

case class ConfigPaths(configPaths: Seq[String]) extends LogSupport {
  info(s"Config file paths: [${configPaths.mkString(", ")}]")

  def findConfigFile(name: String): String = {
    configPaths
    .map(p => new File(p, name))
    .find(_.exists())
    .map(_.getPath)
    .getOrElse(throw new FileNotFoundException(s"${name} is not found"))
  }
}

case class Environment(env: String, defaultEnv: String = "default") {
  override def toString = env
}



