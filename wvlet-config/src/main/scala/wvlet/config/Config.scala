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

import wvlet.inject.Inject
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

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

case class ConfigHolder(env: String, tpe: ObjectType, value: Any)

trait Config extends Iterable[ConfigHolder] {
  def of[ConfigType](implicit tag: ru.TypeTag[ConfigType]): ConfigType
  def bindConfigs(i: Inject)
  def getAll : Seq[ConfigHolder]
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



