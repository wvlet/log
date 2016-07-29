package wvlet

import scala.reflect.ClassTag
import scala.language.experimental.macros

/**
  *
  */
package object helix {

  def inject[A:ClassTag] : A = macro HelixMacros.injectImpl[A]

}


