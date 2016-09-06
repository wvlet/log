/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.config

import java.io.{File, FileInputStream, FileNotFoundException}
import java.util.Properties

import wvlet.config.YamlReader.loadMapOf
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil
import wvlet.obj._

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

case class ConfigHolder(tpe: ObjectType, value: Any)

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

object Config extends LogSupport {
  private def defaultConfigPath = Seq(
    ".", // current directory
    sys.props.getOrElse("prog.home", "") // program home for wvlet-launcher
  )

  def apply(env:String, defaultEnv:String = "default", configPaths:Seq[String]=defaultConfigPath): Config = Config(ConfigEnv(env, "default", configPaths), Map.empty[ObjectType, ConfigHolder])

  private def cleanupConfigPaths(paths: Seq[String]) = {
    val b = Seq.newBuilder[String]
    for (p <- paths) {
      if (!p.isEmpty) {
        b += p
      }
    }
    val result = b.result()
    if (result.isEmpty) {
      Seq(".") // current directory
    }
    else {
      result
    }
  }
}

case class ConfigEnv(env: String, defaultEnv: String, configPaths: Seq[String]) {
  def withConfigPaths(paths: Seq[String]): ConfigEnv = ConfigEnv(env, defaultEnv, paths)
}

case class Config private[config](env: ConfigEnv, holder: Map[ObjectType, ConfigHolder]) extends Iterable[ConfigHolder] with LogSupport {

  // Customization
  def withEnv(newEnv: String, defaultEnv: String = "default"): Config = {
    Config(ConfigEnv(newEnv, defaultEnv, env.configPaths), holder)
  }

  def withConfigPaths(paths: Seq[String]): Config = {
    Config(env.withConfigPaths(paths), holder)
  }

  // Accessors to configurations
  def getAll: Seq[ConfigHolder] = holder.values.toSeq
  override def iterator: Iterator[ConfigHolder] = holder.values.iterator

  private def find[A](tpe: ObjectType): Option[Any] = {
    holder.get(tpe).map(_.value)
  }

  def of[ConfigType](implicit tag: ru.TypeTag[ConfigType]): ConfigType = {
    val t = ObjectType.ofTypeTag(tag)
    find(t) match {
      case Some(x) =>
        x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"No [${t}] value is found")
    }
  }

  def +(h: ConfigHolder): Config = Config(env, this.holder + (h.tpe -> h))
  def ++(other: Config): Config = {
    Config(env, this.holder ++ other.holder)
  }

  def register[ConfigType: ru.TypeTag](config: ConfigType): Config = {
    val tpe = ObjectType.ofTypeTag(implicitly[ru.TypeTag[ConfigType]])
    this + ConfigHolder(tpe, config)
  }

  def registerFromYaml[ConfigType: ru.TypeTag : ClassTag](yamlFile: String): Config = {
    val tpe = ObjectType.ofTypeTag(implicitly[ru.TypeTag[ConfigType]])
    registerFromYaml(yamlFile, TypeUtil.newInstanceOf[ConfigType])
  }

  def registerFromYaml[ConfigType: ru.TypeTag : ClassTag](yamlFile: String, default: => ConfigType): Config = {
    val tpe = ObjectType.ofTypeTag(implicitly[ru.TypeTag[ConfigType]])
    val cls = tpe.rawType

    val yamlFilePath = findConfigFile(yamlFile)
    val configInYaml: Option[ConfigType] = yamlFilePath.flatMap { realPath =>
      val m = loadMapOf[ConfigType](realPath)(ClassTag(cls))
      m.get(env.env) match {
        case Some(x) =>
          info(s"Loading ${tpe} from ${realPath}, env:${env}")
          Some(x)
        case None =>
          // Load default
          debug(s"Configuration for ${env.env} is not found in ${realPath}. Load ${env.defaultEnv} configuration instead")
          info(s"Loading ${tpe} from ${realPath}, env:${env} <= used env:${env.defaultEnv}")
          m.get(env.defaultEnv)
      }
    }
    val newConfig = configInYaml.getOrElse {
      warn(s"Configuration file ${yamlFilePath}")

      default
    }
    this + ConfigHolder(tpe, newConfig)
  }

  def overrideWithProperties(props: Properties): Config = {
    ConfigOverwriter.overrideWithProperties(this, props)
  }

  def overrideWithPropertiesFile(propertiesFile: String): Config = {
    findConfigFile(propertiesFile) match {
      case None =>
        throw new FileNotFoundException(propertiesFile)
      case Some(propPath) =>
        val props = IOUtil.withResource(new FileInputStream(propPath)) { in =>
          val p = new Properties()
          p.load(in)
          p
        }
        overrideWithProperties(props)
    }
  }

  private def findConfigFile(name: String): Option[String] = {
    env.configPaths
    .map(p => new File(p, name))
    .find(_.exists())
    .map(_.getPath)
  }
}
