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

import java.{util => ju}

import org.yaml.snakeyaml.Yaml
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil._
import wvlet.obj.ObjectBuilder

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

object YamlReader extends LogSupport {

  def load[A](resourcePath: String, env: String)(implicit m: ClassTag[A]): A = {
    val map = loadMapOf[A](resourcePath)
    if (!map.containsKey(env)) {
      throw new IllegalArgumentException(s"Env $env is not found in $resourcePath")
    }
    map(env)
  }

  def loadMapOf[A](resourcePath: String)(implicit m: ClassTag[A]): Map[String, A] = {
    val yaml = loadYaml(resourcePath)
    val map = ListMap.newBuilder[String, A]
    for ((k, v) <- yaml) yield {
      map += k.toString -> bind[A](v.asInstanceOf[java.util.Map[AnyRef, AnyRef]])
    }
    map.result
  }

  def loadYaml(resourcePath: String): Map[AnyRef, AnyRef] = {
    new Yaml()
    .load(readAsString(resourcePath))
    .asInstanceOf[ju.Map[AnyRef, AnyRef]]
    .toMap
  }

  def loadYamlList(resourcePath: String): Seq[Map[AnyRef, AnyRef]] = {
    new Yaml()
    .load(readAsString(resourcePath))
    .asInstanceOf[ju.List[ju.Map[AnyRef, AnyRef]]]
    .asScala
    .map(_.asScala.toMap)
    .toSeq
  }

  def bind[A: ClassTag](prop: Map[AnyRef, AnyRef]): A = {
    bind(prop.asJava).asInstanceOf[A]
  }

  def bind[A: ClassTag](prop: java.util.Map[AnyRef, AnyRef]): A = {
    val builder = ObjectBuilder(implicitly[ClassTag[A]].runtimeClass)
    if (prop != null) {
      for ((k, v) <- prop) {
        v match {
          case al: java.util.ArrayList[_] =>
            for (a <- al) {
              builder.set(k.toString, a)
            }
          case _ =>
            builder.set(k.toString, v)
        }
      }
    }
    builder.build.asInstanceOf[A]
  }

}

