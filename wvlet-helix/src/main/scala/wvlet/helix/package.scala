package wvlet

import scala.reflect.ClassTag
import scala.language.experimental.macros

/**
  *
  */
package object helix {

  def inject[A:ClassTag] : A = macro HelixMacros.injectImpl[A]
  def inject[A:ClassTag, D1:ClassTag](factory:D1 => A) : A = macro HelixMacros.inject1Impl[A, D1]
  def inject[A:ClassTag, D1:ClassTag, D2:ClassTag](factory:(D1, D2) => A) : A = macro HelixMacros.inject2Impl[A, D1, D2]
  def inject[A:ClassTag, D1:ClassTag, D2:ClassTag, D3:ClassTag](factory:(D1, D2, D3) => A) : A = macro HelixMacros.inject3Impl[A, D1, D2, D3]
  def inject[A:ClassTag, D1:ClassTag, D2:ClassTag, D3:ClassTag, D4:ClassTag](factory:(D1, D2, D3, D4) => A) : A = macro HelixMacros.inject4Impl[A, D1, D2, D3, D4]
  def inject[A:ClassTag, D1:ClassTag, D2:ClassTag, D3:ClassTag, D4:ClassTag, D5:ClassTag](factory:(D1, D2, D3, D4, D5) => A) : A = macro HelixMacros.inject5Impl[A, D1, D2, D3, D4, D5]

}


