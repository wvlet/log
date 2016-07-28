package wvlet.helix

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.{Context, _}

class service extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ServiceMacro.impl

}

object ServiceMacro {



  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def extractClassNameAndFields(classDecl: ClassDef) = {
      try {
        val q"$clsType trait $className extends ..$bases { ..$body }" = classDecl
        (className, body)
      } catch {
        case _: MatchError => c.abort(c.enclosingPosition, "Annotation is only supported on trait class")
      }
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: Nil =>
        println(classDecl.toString())
        val (className, body) = extractClassNameAndFields(classDecl)
        val serviceName = TypeName(s"${className.toString()}Service2")
        val localName = TermName(s"${className.toString().toLowerCase}")
        c.Expr(
          q"""
            $classDecl

            trait $serviceName {
               protected def $localName = wvlet.helix.inject[$className]
            }
            """
        )
      //case (classDecl: ClassDef) :: Nil => modifiedClass(c, classDecl)
      //case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil => modifiedClass(c, classDecl, Some(compDecl))
      case _ =>
        c.abort(c.enclosingPosition, "@service cannot be used other than trait or class")
    }
  }

}