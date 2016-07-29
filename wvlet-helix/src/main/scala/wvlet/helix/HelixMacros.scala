package wvlet.helix

import wvlet.helix.HelixException.MISSING_CONTEXT
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.reflect.{macros => sm}
import scala.util.Try
/**
  *
  */
object HelixMacros {

  def findContext[A](enclosingObj:A) : Context = {
    val cl = enclosingObj.getClass

    def returnsContext(c:Class[_]) = {
      classOf[wvlet.helix.Context].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.helix.Context
    val schema = ObjectSchema(cl)
    schema
    .methods
    .find(x => returnsContext(x.valueType.rawType) && x.params.isEmpty)
    .flatMap(contextGetter => Try(contextGetter.invoke(enclosingObj.asInstanceOf[AnyRef]).asInstanceOf[Context]).toOption)
    .orElse{
      // Find parameters
      schema
      .parameters
      .find(p => returnsContext(p.valueType.rawType))
      .flatMap(contextParam => Try(contextParam.get(enclosingObj).asInstanceOf[Context]).toOption)
    }
    .getOrElse {
      // Find any embedded context
      val m = cl.getDeclaredMethod("__helix_context")
      if (m != null) {
        m.invoke(enclosingObj).asInstanceOf[Context]
      }
      else {
        error(s"No wvlet.helix.Context is found in the scope: ${ObjectType.of(cl)}")
        throw new HelixException(MISSING_CONTEXT(ObjectType.of(cl)))
      }
    }
  }

  def injectImpl[A:c.WeakTypeTag](c:sm.Context)(ev:c.Tree) : c.Expr[A] = {
    import  c.universe._

    c.Expr(
      q"""{
         val c = wvlet.helix.HelixMacros.findContext(this)
         c.get(${ev})
        }
      """
    )
  }

  def weaveImpl[A:c.WeakTypeTag](c:sm.Context)(ev:c.Tree) : c.Expr[A] = {
    import c.universe._

    val t = ev.tpe.typeArgs(0)
    c.Expr(
    q"""
       new $t {
         protected def __helix_context = ${c.prefix}
       }
      """
    )
  }
}
