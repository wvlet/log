package wvlet.config

import java.io._
import java.nio.charset.StandardCharsets
import java.{util => ju}

import org.yaml.snakeyaml.Yaml
import wvlet.log.LogSupport
import wvlet.obj.ObjectBuilder

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

object YamlReader extends LogSupport {

  private def withResource[Resource <: AutoCloseable, U](resource: Resource)(body: Resource => U): U = {
    try {
      body(resource)
    }
    finally {
      resource.close
    }
  }

  private def readAsString(resourcePath: String) = {
    require(resourcePath != null, s"resourcePath is null")
    val file = findPath(new File(resourcePath))
    if (!file.exists()) {
      throw new FileNotFoundException(s"${file} is not found")
    }
    readFully(new FileInputStream(file)) {
      data => new String(data, StandardCharsets.UTF_8)
    }
  }

  private def readFully[U](in: InputStream)(f: Array[Byte] => U): U = {
    val byteArray = withResource(new ByteArrayOutputStream) {
      b =>
        val buf = new Array[Byte](8192)
        withResource(in) {
          src =>
            var readBytes = 0
            while ( {
              readBytes = src.read(buf);
              readBytes != -1
            }) {
              b.write(buf, 0, readBytes)
            }
        }
        b.toByteArray
    }
    f(byteArray)
  }

  private def findPath(path: String): File = findPath(new File(path))

  private def findPath(path: File): File = {
    if (path.exists()) {
      path
    }
    else {
      val defaultPath = new File(new File(System.getProperty("prog.home", "")), path.getPath)
      if (!defaultPath.exists()) {
        throw new FileNotFoundException(s"${path} is not found")
      }
      defaultPath
    }
  }

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

