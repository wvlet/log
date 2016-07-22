package wvlet.config

import org.yaml.snakeyaml.Yaml
import wvlet.log.LogSupport
import java.{util => ju}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag
import wvlet.core.io.IOUtil._
import wvlet.obj.ObjectBuilder

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

  def bind[A](prop: java.util.Map[AnyRef, AnyRef])(implicit m: ClassTag[A]): A = {
    val builder = ObjectBuilder(m.runtimeClass.asInstanceOf[Class[A]])
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
    builder.build
  }

}

