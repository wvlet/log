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

import wvlet.config.IOUtil._
import wvlet.config.YamlReader.loadMapOf
import wvlet.log.LogSupport
import wvlet.obj.ObjectBuilder.CanonicalNameFormatter
import wvlet.obj.{ObjectBuilder, ObjectSchema, ObjectType, TaggedObjectType}

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Success, Try}

object ConfigBuilder extends LogSupport {
  def apply(env: String, configPaths: String) = new ConfigBuilder(Environment(env), configPaths.split(":"))



  private[config] def extractPrefix(t: ObjectType): ConfigPrefix = {

    def canonicalize(s:String) : String = {
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
  case class ConfigParam(key:ConfigParamKey, v:Any)

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

import wvlet.config.ConfigBuilder._

/**
  *
  */
class ConfigBuilder(env: Environment, configPaths: Seq[String]) extends LogSupport {

  require(!configPaths.isEmpty, "configPaths is empty")

  private val configHolder = Seq.newBuilder[ConfigHolder]

  private def findConfigFile(name: String): String = {
    configPaths
    .map(p => new File(p, name))
    .find(_.exists())
    .map(_.getPath)
    .getOrElse(throw new FileNotFoundException(s"${name} is not found"))
  }

  def addAll(config: Config): ConfigBuilder = {
    configHolder ++= config
    this
  }

  def add(config: ConfigHolder): ConfigBuilder = {
    configHolder += config
    this
  }

  def register[ConfigType](config: ConfigType)(implicit tag: ru.TypeTag[ConfigType]): ConfigBuilder = {
    val tpe = ObjectType.ofTypeTag(tag)
    configHolder += ConfigHolder(env.env, tpe, config)
    this
  }

  def registerFromYaml[ConfigType](configFilePath: String)
                                  (implicit tag: ru.TypeTag[ConfigType]): ConfigBuilder = {
    val tpe = ObjectType.ofTypeTag(tag)
    val cls = tpe.rawType
    val realPath = findConfigFile(configFilePath)

    val m = loadMapOf[ConfigType](realPath)(ClassTag(cls))
    val config = m.get(env.env) match {
      case Some(x) =>
        info(s"Loading ${tpe} config from ${realPath}, env:${env}")
        x
      case None =>
        // Load default
        debug(s"Configuration for ${env.env} is not found in ${realPath}. Load ${env.defaultEnv} configuration instead")
        info(s"Loading ${tpe} from ${realPath}, env:${env} <= used env:${env.defaultEnv}")
        m.getOrElse(
          env.defaultEnv, {
            val m = s"No config for ${tpe} is found for ${env.defaultEnv} within ${realPath}"
            error(m)
            throw new IllegalArgumentException(m)
          }
        )
    }
    configHolder += ConfigHolder(env.env, tpe, config)
    this
  }

  def overrideWithPropertiesFile(propertiesFile: String): ConfigBuilder = {
    val propPath = findConfigFile(propertiesFile)
    val props = withResource(new FileInputStream(propertiesFile)) { in =>
      val p = new Properties()
      p.load(in)
      p
    }
    overrideWithProperties(props)
  }

  def overrideWithProperties(props:Properties) : ConfigBuilder = {
    val overrides = {
      val b = Seq.newBuilder[ConfigParam]
      for ((k, v) <- props) yield {
        val key = toConfigKey(k)
        b += ConfigParam(key, v)
      }
      b.result
    }

    val currentConfigs = configHolder.result()
    configHolder.clear()
    for (c <- currentConfigs) {
      val b = ObjectBuilder.fromObject(c.value)
      val prefix = extractPrefix(c.tpe)
      val overrideParams = overrides.filter(_.key.prefix == prefix)
      for(p <- overrideParams) {
        trace(s"override: ${p}")
        b.set(p.key.param, p.v)
      }
      val newConfig = b.build
      configHolder += ConfigHolder(c.env, c.tpe, newConfig)
    }

    this
  }

  def build: Config = {
    // Override previous occurrences of the same type config
    val b = Map.newBuilder[ObjectType, ConfigHolder]
    for (s <- configHolder.result) {
      b += s.tpe -> s
    }
    new ConfigImpl(b.result().values.toIndexedSeq)
  }
}
