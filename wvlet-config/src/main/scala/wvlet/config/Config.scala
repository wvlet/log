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
import wvlet.obj.ObjectBuilder.CanonicalNameFormatter
import wvlet.obj._

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Success, Try}

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

case class Environment(env: String, defaultEnv: String = "default") {
  override def toString = env
}

object Config extends LogSupport {
  private def defaultConfigPath = Seq(
    ".", // current directory
    sys.props.getOrElse("prog.home", "") // program home
  )

  def apply(env: String, configPaths: String): Config =
    Config(Environment(env), cleanupConfigPaths(configPaths.split(":")), Vector.empty)

  def apply(env: String, configPaths: Seq[String] = defaultConfigPath): Config =
    Config(Environment(env), cleanupConfigPaths(configPaths), Vector.empty)

  def apply(env: Environment, configPaths: Seq[String]): Config =
    Config(env, cleanupConfigPaths(configPaths), Vector.empty)

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

  private[config] def extractPrefix(t: ObjectType): ConfigPrefix = {
    def canonicalize(s: String): String = {
      val name = s.replaceAll("Config$", "")
      CanonicalNameFormatter.format(name)
    }

    t match {
      case TaggedObjectType(raw, base, taggedType) =>
        ConfigPrefix(Some(CanonicalNameFormatter.format(taggedType.name)), canonicalize(base.name))
      case _ =>
        ConfigPrefix(None, canonicalize(t.name))
    }
  }

  case class ConfigPrefix(tag: Option[String], prefix: String)
  case class ConfigParamKey(prefix: ConfigPrefix, param: String)
  case class ConfigParam(key: ConfigParamKey, v: Any)

  private[config] def configToProps(configHolder: ConfigHolder): Seq[ConfigParam] = {
    val prefix = extractPrefix(configHolder.tpe)
    val schema = ObjectSchema.of(configHolder.tpe)
    val b = Seq.newBuilder[ConfigParam]
    for (p <- schema.parameters) yield {
      val key = ConfigParamKey(prefix, CanonicalNameFormatter.format(p.name))
      Try(p.get(configHolder.value)) match {
        case Success(v) => b += ConfigParam(key, v)
        case Failure(e) =>
          warn(s"Failed to read parameter ${p} from ${configHolder}")
      }
    }
    b.result()
  }

  private[config] def toConfigKey(propKey: String): ConfigParamKey = {
    val c = propKey.split("\\.")
    c.length match {
      case l if l >= 3 =>
        val tag = c(0)
        val prefix = c(1)
        val param = CanonicalNameFormatter.format(c.drop(2).mkString)
        ConfigParamKey(ConfigPrefix(Some(tag), prefix), param)
      case l if l == 2 =>
        val prefix = c(0)
        val param = CanonicalNameFormatter.format(c(1))
        ConfigParamKey(ConfigPrefix(None, prefix), param)
      case other =>
        throw new IllegalArgumentException(s"${propKey} should have ([tag].)?[prefix].[param] format")
    }
  }
}

import wvlet.config.Config._
case class Config(env: Environment, configPaths: Seq[String], holder: Vector[ConfigHolder]) extends Iterable[ConfigHolder] with LogSupport {
  def getAll: Seq[ConfigHolder] = holder
  override def iterator: Iterator[ConfigHolder] = holder.iterator

  private def find[A](tpe: ObjectType): Option[Any] = {
    holder.filter(_.tpe == tpe).lastOption.map(_.value)
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

  def +(holder: ConfigHolder): Config = Config(env, configPaths, this.holder :+ holder)
  def ++(other: Config): Config = {
    Config(env, configPaths, this.holder ++ other.holder)
  }

  def register[ConfigType: ru.TypeTag](config: ConfigType): Config = {
    val tpe = ObjectType.ofTypeTag(implicitly[ru.TypeTag[ConfigType]])
    this + ConfigHolder(tpe, config)
  }

  def registerFromYaml[ConfigType: ru.TypeTag : ClassTag](yamlFile: String) : Config = {
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
    val overrides = {
      import scala.collection.JavaConversions._
      val b = Seq.newBuilder[ConfigParam]
      for ((k, v) <- props) yield {
        val key = toConfigKey(k)
        b += ConfigParam(key, v)
      }
      b.result
    }

    val newConfigs = for (c <- holder) yield {
      val configBuilder = ObjectBuilder.fromObject(c.value)
      val prefix = extractPrefix(c.tpe)
      val overrideParams = overrides.filter(_.key.prefix == prefix)
      for (p <- overrideParams) {
        trace(s"override: ${p}")
        configBuilder.set(p.key.param, p.v)
      }
      ConfigHolder(c.tpe, configBuilder.build)
    }

    Config(env, configPaths, newConfigs)
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
    configPaths
    .map(p => new File(p, name))
    .find(_.exists())
    .map(_.getPath)
  }
}
