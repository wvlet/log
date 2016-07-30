package wvlet

import scala.reflect.runtime.{universe=>ru}
import scala.language.experimental.macros

/**
  *
  */
package object helix {

  def inject[A:ru.TypeTag] : A = macro HelixMacros.injectImpl[A]
  def inject[A:ru.TypeTag, D1:ru.TypeTag](factory:D1 => A) : A = macro HelixMacros.inject1Impl[A, D1]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag](factory:(D1, D2) => A) : A = macro HelixMacros.inject2Impl[A, D1, D2]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag, D3:ru.TypeTag](factory:(D1, D2, D3) => A) : A = macro HelixMacros.inject3Impl[A, D1, D2, D3]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag, D3:ru.TypeTag, D4:ru.TypeTag](factory:(D1, D2, D3, D4) => A) : A = macro HelixMacros.inject4Impl[A, D1, D2, D3, D4]
  def inject[A:ru.TypeTag, D1:ru.TypeTag, D2:ru.TypeTag, D3:ru.TypeTag, D4:ru.TypeTag, D5:ru.TypeTag](factory:(D1, D2, D3, D4, D5) => A) : A = macro HelixMacros.inject5Impl[A, D1, D2, D3, D4, D5]

}


