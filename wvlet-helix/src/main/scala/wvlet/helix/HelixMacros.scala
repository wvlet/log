package wvlet.helix

import scala.reflect.{macros => sm}
/**
  *
  */
object HelixMacros {

  def findContext[A](enclosingObj:A) : Context = {
    val cl = enclosingObj.getClass
    val m = cl.getDeclaredMethod("_context")
    val context = m.invoke(enclosingObj).asInstanceOf[Context]
    context
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

  def weave[A:c.WeakTypeTag](c:sm.Context)(ev:c.Tree) : c.Expr[A] = {
    import c.universe._

    val t = ev.tpe.typeArgs(0)
    c.Expr(
    q"""
       new $t {
         protected def _context = ${c.prefix}
       }
      """
    )
  }
}
