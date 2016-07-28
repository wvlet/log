package wvlet.helix

import java.lang.reflect.Field

import sun.misc.Unsafe
import wvlet.log.LogSupport

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag

object Helix {




}



import wvlet.helix.Helix._

/**
  *
  */
class Helix extends LogSupport {

  def build[A](implicit ev: ClassTag[A]): A = {
    val cl = ev.runtimeClass
    info(s"build ${cl.getName}")

    val m = cl.getDeclaredMethods()
    info(m.mkString("\n"))


    null.asInstanceOf[A]
  }

}

