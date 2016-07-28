package wvlet

import scala.reflect.ClassTag
import scala.language.experimental.macros

/**
  *
  */
package object helix {

  def inject[A:ClassTag] : A = macro HelixMacros.injectImpl[A]

  /**
    * Context tracks the dependencies of objects and use them to instanciate objects
    */
  trait Context {

    /**
      * Creates an instance of the given type A.
      *
      * @tparam A
      * @return object
      */
    def get[A:ClassTag] : A

    def weave[A:ClassTag] : A = macro HelixMacros.weave[A]

  }

}


