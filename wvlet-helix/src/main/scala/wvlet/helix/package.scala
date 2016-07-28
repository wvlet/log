package wvlet

import scala.reflect.ClassTag

/**
  *
  */
package object helix {

  def inject[A:ClassTag] : A = null.asInstanceOf[A]

}
