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

import java.util.Properties

import wvlet.log.LogSupport
import wvlet.obj.ObjectBuilder.CanonicalNameFormatter
import wvlet.obj.{ObjectBuilder, ObjectSchema, ObjectType, TaggedObjectType}

import scala.util.{Failure, Success, Try}

/**
  *
  */
object ConfigOverwriter extends LogSupport {

  private[config] def extractPrefix(t: ObjectType): ParamKey = {
    def canonicalize(s: String): String = {
      val name = s.replaceAll("Config$", "")
      CanonicalNameFormatter.format(name)
    }

    t match {
      case TaggedObjectType(raw, base, taggedType) =>
        ParamKey(canonicalize(base.name), Some(CanonicalNameFormatter.format(taggedType.name)))
      case _ =>
        ParamKey(canonicalize(t.name), None)
    }
  }

  case class ParamKey(preefix:String, tag: Option[String])
  case class ConfigParamKey(prefix: ParamKey, param: String)
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
      case l if l >= 2 =>
        val prefixSplit = c(0).split("@")
        if(prefixSplit.length > 1) {
          val param = CanonicalNameFormatter.format(c(1).mkString)
          ConfigParamKey(ParamKey(prefixSplit(0), Some(prefixSplit(1))), param)
        }
        else {
          val prefix = c(0)
          val param = CanonicalNameFormatter.format(c(1))
          ConfigParamKey(ParamKey(prefix, None), param)
        }
      case other =>
        throw new IllegalArgumentException(s"${propKey} should have [prefix](@[tag])?.[param] format")
    }
  }

  def overrideWithProperties(config:Config, props: Properties): Config = {
    val overrides = {
      import scala.collection.JavaConversions._
      val b = Seq.newBuilder[ConfigParam]
      for ((k, v) <- props) yield {
        val key = toConfigKey(k)
        val p = ConfigParam(key, v)
        b += p
      }
      b.result
    }

    var unusedProperties : Set[ConfigParamKey] = overrides.map(_.key).toSet

    val newConfigs = for (c <- config.holder) yield {
      val configBuilder = ObjectBuilder.fromObject(c.value)
      val prefix = extractPrefix(c.tpe)
      val overrideParams = overrides.filter(_.key.prefix == prefix)
      for (p <- overrideParams) {
        trace(s"override: ${p}")
        unusedProperties -= p.key
        configBuilder.set(p.key.param, p.v)
      }
      ConfigHolder(c.tpe, configBuilder.build)
    }

    Config(config.env, config.configPaths, newConfigs)
  }

}
