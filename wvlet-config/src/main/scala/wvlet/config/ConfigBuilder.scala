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

import java.io.{File, FileNotFoundException}

import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

trait ConfigBuilder {
  def build: Config
  def registerFromYaml[ConfigType: ru.TypeTag](configFilePath: String): ConfigBuilder
  def register[ConfigType: ru.TypeTag](config: ConfigType): ConfigBuilder
  def addAll(config: Config): ConfigBuilder
  def add(config: ConfigHolder): ConfigBuilder
}

private[config] case class ConfigHolder(env: String, tpe: ObjectType, value: Any)
/**
  *
  */
class ConfigBuilderImpl(env: Environment, configPaths: Seq[String]) extends ConfigBuilder with LogSupport {
  info(s"Config file paths: [${configPaths.mkString(", ")}]")
  require(!configPaths.isEmpty, "configPaths is empty")

  private var configHolder = Seq.newBuilder[ConfigHolder]

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

  def build: Config = {
    // Override previous occurrences of the same type config
    val b = Map.newBuilder[ObjectType, ConfigHolder]
    for (s <- configHolder.result) {
      b += s.tpe -> s
    }
    new ConfigImpl(b.result().values.toIndexedSeq)
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
    info(s"Loading ${tpe} config from ${realPath}, env:${env}")
    val m = YamlReader.loadMapOf[ConfigType](realPath)(ClassTag(cls))
    val config = m.get(env.env) match {
      case Some(x) => x
      case None =>
        // Load default
        warn(s"Configuration for ${env.env} is not found in ${realPath}. Load ${env.defaultEnv} configuration instead")
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
}
