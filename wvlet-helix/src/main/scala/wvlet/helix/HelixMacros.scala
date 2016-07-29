package wvlet.helix

import wvlet.helix.HelixException.MISSING_CONTEXT
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType}

import scala.reflect.{macros => sm}
import scala.util.Try
/**
  *
  */
object HelixMacros extends LogSupport {

  def findContext[A](enclosingObj:A) : Context = {
    val cl = enclosingObj.getClass

    def returnsContext(c:Class[_]) = {
      classOf[wvlet.helix.Context].isAssignableFrom(c)
    }

    // find val or def that returns wvlet.helix.Context
    val schema = ObjectSchema(cl)

    def findContextFromMethods : Option[Context] = {
      schema
      .methods
      .find(x => returnsContext(x.valueType.rawType) && x.params.isEmpty)
      .flatMap{contextGetter =>
        Try(contextGetter.invoke(enclosingObj.asInstanceOf[AnyRef]).asInstanceOf[Context]).toOption
      }
    }

    def findContextFromParams : Option[Context] = {
      // Find parameters
      schema
      .parameters
      .find(p => returnsContext(p.valueType.rawType))
      .flatMap{ contextParam => Try(contextParam.get(enclosingObj).asInstanceOf[Context]).toOption}
    }

    def findEmbeddedContext : Option[Context] = {
      // Find any embedded context
      val m = Try(cl.getDeclaredMethod("__helix_context")).toOption
      m.flatMap { m =>
        Try(m.invoke(enclosingObj).asInstanceOf[Context]).toOption
      }
    }

    findContextFromMethods
    .orElse(findContextFromParams)
    .orElse(findEmbeddedContext)
    .getOrElse {
      error(s"No wvlet.helix.Context is found in the scope: ${ObjectType.of(cl)}")
      throw new HelixException(MISSING_CONTEXT(ObjectType.of(cl)))
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
