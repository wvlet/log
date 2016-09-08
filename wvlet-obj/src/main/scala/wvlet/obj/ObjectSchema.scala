/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.obj

import java.lang.{reflect => jr}
import java.{lang => jl}

import wvlet.log.LogSupport
import wvlet.obj.ObjectBuilder.CanonicalNameFormatter

import scala.collection.mutable.WeakHashMap
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.reflect.internal.MissingRequirementError
import scala.tools.scalap.scalax.rules.scalasig._
import scala.util.{Failure, Success, Try}


/**
  * Contains information of methods, constructor and parameters defined in a class
  *
  * @author leo
  */
case class ObjectSchema(cl: Class[_], parameters: Seq[Parameter]) extends LogSupport {

  if (cl == null) {
    throw new NullPointerException("input class is null")
  }

  import ObjectSchema._

  val name    : String = cl.getSimpleName
  val fullName: String = cl.getName

  def findSignature: Option[ScalaSig] = ObjectSchema.findSignature(cl)

  lazy val methods: Seq[ObjectMethod] = publicMethodsOf(cl)
  lazy val allMethods : Seq[ObjectMethod] = allMethodsOf(cl)

  lazy private val parameterIndex: Map[String, Parameter] = {
    val pair = for (a <- parameters) yield CanonicalNameFormatter.format(a.name) -> a
    pair.toMap
  }

  def getParameter(name: String): Parameter = {
    parameterIndex(CanonicalNameFormatter.format(name))
  }
  def findParameter(name: String): Option[Parameter] = {
    parameterIndex.get(CanonicalNameFormatter.format(name))
  }
  def containsParameter(name: String) = parameterIndex.contains(CanonicalNameFormatter.format(name))

  lazy val constructor: Constructor = {
    findConstructor match {
      case Some(c) => c
      case None => throw new IllegalArgumentException("no constructor is found for " + cl)
    }
  }
  def findConstructor: Option[Constructor] = ObjectSchema.findConstructor(cl)

  override def toString = {
    findConstructor.map {
      cc =>
        if (cc.params.isEmpty) {
          name
        }
        else {
          "%s(%s)".format(name, cc.params.mkString(", "))
        }
    } getOrElse name
  }
}




/**
  * Object information extractor
  * @since 2012/01/17 10:05
  */
object ObjectSchema extends LogSupport {

  import TypeUtil._

  private val schemaTable = new WeakHashMap[Class[_], ObjectSchema]
  private val sigCache = new WeakHashMap[Class[_], Option[ScalaSig]]

  /**
    * Get the object schema of the specified ObjectType. This method caches previously created ObjectSchema instances, and
    * second call for the same ObjectType object return the cached entry.
    */
  def apply(cl: Class[_]): ObjectSchema = schemaTable.getOrElseUpdate(cl, createSchema(cl))

  def of(tpe: ObjectType) : ObjectSchema = ObjectSchema(tpe.rawType)

  private def createSchema(cl: Class[_]): ObjectSchema = {
    trace(s"createSchema of ${cl}")
    new ObjectSchema(cl, parametersOf(cl))
  }

  def of[A](implicit m: ClassTag[A]): ObjectSchema = apply(m.runtimeClass)
  def getSchemaOf(obj: Any): ObjectSchema = apply(cls(obj))

  private def findClass(name: String): Option[Class[_]] = {
    try {
      Some(Class.forName(name))
    }
    catch {
      case e: Exception => None
    }
  }

  /**
    * Find the Scala signature of the specified class
    *
    * @param cl
    * @return scala signature of the class
    */
  def findSignature(cl: Class[_]): Option[ScalaSig] = {
    trace(s"find signature: $cl")
    // Find an enclosing object containing the target class definition
    def enclosingObject(cl: Class[_]): Option[Class[_]] = {
      val pos = cl.getName.lastIndexOf("$")
      val parentClassName = cl.getName.slice(0, pos)
      findClass(parentClassName)
    }

    sigCache.getOrElseUpdate(cl, {
      val sig =
        try {
          trace(s"Searching for signature of ${cl.getName}")
          val s = ScalaSigParser.parse(cl)
          if (s.isDefined) {
            trace(s"Found signature of ${cl.getSimpleName}")
          }
          s
        }
        catch {
          // ScalaSigParser throws NPE when noe signature for the class is found
          case e: Exception =>
            trace(s"$cl ${e.getMessage}")
            None
        }
      // If no signature is found, search an enclosing object
      sig.orElse(enclosingObject(cl).flatMap(findSignature(_)))
    })
  }

  def findFieldOwner(name: String, baseClass: Class[_]): Option[Class[_]] = {
    def isFieldOwner(cl: Class[_]) = {
      try {
        cl.getDeclaredField(name)
        true
      }
      catch {
        case _: Exception => false
      }
    }

    trace(s"find field owner: $name, $baseClass")
    if (isFieldOwner(baseClass)) {
      Some(baseClass)
    }
    else {
      val parents = findParentClasses(baseClass)
      parents.foldLeft[Option[Class[_]]](None) {
        (opt, cl) => opt.orElse(findFieldOwner(name, cl))
      }
    }
  }

  private def findConstructor(cl: Class[_], sig: ScalaSig): Option[Constructor] = {
    trace(s"findConstructor: $cl")
    import scala.reflect.runtime.{universe => ru}
    import ru._
    val mirror = ru.runtimeMirror(Thread.currentThread().getContextClassLoader)
    val cs = try {
      mirror.staticClass(cl.getCanonicalName)
    }
    catch {
      case e: MissingRequirementError => {
        val md = mirror.staticModule(cl.getEnclosingClass.getCanonicalName)
        md.typeSignature.member(TypeName(cl.getSimpleName))
      }
    }
    val cc = cs.typeSignature.decl(ru.termNames.CONSTRUCTOR)
    if (!cc.isMethod) {
      None
    }
    else {
      val fstParen = cc.asMethod.paramLists.headOption.getOrElse(Seq.empty)
      val ccParams = for ((p, i) <- fstParen.zipWithIndex) yield {
        val name = p.name.decodedName.toString
        val tpe = p.typeSignature
        ConstructorParameter(cl, findFieldOwner(name, cl), i, name, ObjectType.fromType(tpe))
      }
      val ctor = Constructor(cl, ccParams.toArray)
      Some(ctor)
    }

//    import scala.tools.scalap.scalax.rules.scalasig
//
//    def isTargetClass(t: scalasig.Type): Boolean = {
//      t match {
//        case TypeRefType(_, c@ClassSymbol(sinfo, _), _) => {
//          // when ClassSymbol contains isModule flag, it is a constructor of the companion object
//          sinfo.name == cl.getSimpleName && !c.isModule
//        }
//        case _ => false
//      }
//    }
//    def findConstructorParameters(mt: MethodType): Array[ConstructorParameter] = {
//      trace(s"constructor method type: $mt")
//      val paramSymbols: Seq[MethodSymbol] = mt match {
//        case MethodType(_, param: Seq[_]) => param.collect {
//          case m: MethodSymbol => m
//        }
//        case _ => Seq.empty
//      }
//
//      val l = for (((name, vt), index) <- toAttribute(paramSymbols, sig, cl).zipWithIndex)
//      yield {
//        // resolve the actual field owner
//        val fieldOwner = findFieldOwner(name, cl)
//
//        // This error happens when a private val field is defined in the constructor, but never used in the class body
//        //if (fieldOwner.isEmpty)
//        //     throw new IllegalStateException("No field owner is found: name:%s, base class:%s".format(name, cl.getSimpleName))
//        ConstructorParameter(cl, fieldOwner, index, name, vt)
//      }
//      l.toArray
//    }
//
//
//
//    val entries = (0 until sig.table.length).map(sig.parseEntry(_))
//    val ctors = entries.collect {
//      case m: MethodType if isTargetClass(m.resultType) => {
//        val params = findConstructorParameters(m)
//        Constructor(cl, params)
//      }
//    }
//    if (ctors.isEmpty)
//      None
//    else {
//      trace(s"multiple constructors found:\n${ctors.mkString("\n")}")
//      ctors.headOption
//    }
  }

  private def findConstructor(cl: Class[_]): Option[Constructor] = {
    try
        for (sig <- findSignature(cl); cc <- findConstructor(cl, sig)) yield cc
    catch {
      case e: Exception =>
        error(e)
        None
    }
  }

  private def isSystemClass(cl: Class[_]) = {
    if (cl == null) {
      true
    }
    else {
      val name = cl.getName
      (name.startsWith("java.") || name == "scala" || name.startsWith("scala."))
    }
  }

  def getParentsByReflection(cl: Class[_]) = {
    val i = if (cl.getInterfaces != null) cl.getInterfaces else Array.empty[Class[_]]
    val p = Seq(cl.getSuperclass) ++ i
    val filtered = p.filterNot(c => isSystemClass(c))
    if (!filtered.isEmpty) {
      trace(s"parents of ${cl.getSimpleName}: ${filtered.map(_.getName).mkString(", ")}")
    }
    filtered
  }

  def findParentClasses(cl: Class[_]): Seq[Class[_]] = getParentsByReflection(cl)

  def findParentSchemas(cl: Class[_]): Seq[ObjectSchema] = {
    findParentClasses(cl).map(ObjectSchema(_))
  }

  private def toAttribute(param: Seq[MethodSymbol], sig: ScalaSig, refCl: Class[_]): Seq[(String, ObjectType)] = {
    val paramRefs = param.map(p => (p.name, sig.parseEntry(p.symbolInfo.info)))
    trace(s"method param refs:\n${paramRefs.mkString("\n")}")
    paramRefs.map {
      //      case (name: String, t @ TypeRefType(prefix, symbol, Seq(tp : TypeRefType))) =>
      //        (name, resolveClass(tp))
      case (name: String, t: TypeRefType) =>
        (name, resolveClass(sig, t))
      case (name: String, ExistentialType(tref: TypeRefType, symbols)) =>
        (name, resolveClass(sig, tref))
    }
  }

  def isOwnedByTargetClass(m: MethodSymbol, cl: Class[_]): Boolean = {
    val sn = cl.getSimpleName
    m.symbolInfo.owner match {
//      case cs @ ClassSymbol(symbolInfo, _) if cs.isModule => {
//        val n = if(sn.endsWith("$")) sn.dropRight(1) else sn
//        symbolInfo.name == n
//      }
      case cs@ClassSymbol(symbolInfo, _) => symbolInfo.name == sn
      case _ => false
    }
  }

  private def parseEntries(sig: ScalaSig) = (0 until sig.table.length).map(sig.parseEntry(_))

  /**
    * Find parameters defined in the class
    *
    * @param cl
    * @return
    */
  def parametersOf(cl: Class[_]): Seq[Parameter] = {
    findSignature(cl) match {
      case None => Seq.empty
      case Some(sig) => {
        val entries = parseEntries(sig)

        val parents = findParentSchemas(cl)
        val parentParams = parents.flatMap {
          p => p.parameters
        }.collect {
          //case c @ ConstructorParameter(owner, fieldOwner, index, name, valueType) => c
          //case m @ MethodParameter(owner, index, name, valueType) => m
          // Fix actual owner
          case FieldParameter(owner, ref, name, valueType) if findFieldOwner(name, cl).isDefined => {
            val fieldOwner = findFieldOwner(name, cl).get
            FieldParameter(fieldOwner, cl, name, valueType)
          }
        }

        val constructorParams = findConstructor(cl, sig) match {
          case None => Seq.empty[ConstructorParameter]
          case Some(cc) => cc.params.toSeq
        }

        def isFieldReader(m: MethodSymbol): Boolean = {
          m.isAccessor && !m.isParamAccessor && !m.isLazy && isOwnedByTargetClass(m, cl) && !m.name.endsWith("_$eq") &&
            m.symbolInfo.privateWithin.isEmpty
        }

        val fieldParams = entries.collect {
          case m: MethodSymbol if isFieldReader(m) => {
            entries(m.symbolInfo.info) match {
              case NullaryMethodType(resultType: TypeRefType) =>
                Some(FieldParameter(cl, cl, m.name, resolveClass(sig, resultType)))
              case other =>
                None
            }
          }
        }.collect { case Some(x) => x }

        // Aggregate parameters
        val foundParams = (constructorParams.map(_.name) ++ fieldParams.map(_.name)).toSet
        val parentOnlyParams = parentParams.filterNot(p => foundParams.contains(p.name))

        (constructorParams ++ fieldParams ++ parentOnlyParams)
      }
    }
  }

  def publicMethodsOf(cl: Class[_]): Array[ObjectMethod] = {
    methodsOf(cl, { m => !m.isPrivate && !m.isProtected })
  }

  def allMethodsOf(cl:Class[_]) : Array[ObjectMethod] = {
    methodsOf(cl, { m => true })
  }

  def methodsOf(cl: Class[_], methodFilter: MethodSymbol => Boolean): Array[ObjectMethod] = {
    val methods = findSignature(cl) map {
      sig =>
        val entries = (0 until sig.table.length).map(sig.parseEntry(_))

        def isTargetMethod(m: MethodSymbol): Boolean = {
          // synthetic is used for functions returning default values of method arguments (e.g., ping$default$1)
          m.isMethod && methodFilter(m) && !m.isImplicit && !m.isSynthetic && !m.isAccessor && m.name != "<init>" &&
            m.name != "$init$" && isOwnedByTargetClass(m, cl)
        }

        def resolveMethodArgTypes(params: Seq[(String, ObjectType)]) = {
          params.map {
            case (name, vt) if TypeUtil.isArray(vt.rawType) => {
              val gt = vt.asInstanceOf[GenericType]
              val t = gt.genericTypes(0)
              val loader = Thread.currentThread.getContextClassLoader
              Class.forName("[L%s;".format(t.rawType.getName), false, loader)
            }
            case (name, vt) =>
              vt.rawType
          }
        }

        val methods = entries.collect { case m: MethodSymbol => m }
        //trace(s"methods:\n${methods.mkString("\n")}")
        val targetMethodSymbol: Seq[(MethodSymbol, Any)] = methods.collect {
          case m: MethodSymbol if isTargetMethod(m) => (m, entries(m.symbolInfo.info))
        }
        //trace(s"target methods:\n${targetMethodSymbol.mkString("\n")}")

        def isAccessibleParams(params: Seq[MethodSymbol]): Boolean = {
          params.forall(p => !p.isByNameParam)
        }


        def resolveMethod(cl: Class[_], name: String, resultType: TypeRefType, params: Seq[(String, ObjectType)],
                          paramTypes: Class[_]*): Option[ObjectMethod] = {
          try {
            val mt = cl.getMethod(name, paramTypes: _*)
            val mp = for (((name, vt), index) <- params.zipWithIndex) yield MethodParameter(mt, index, name, vt)
            Some(ScMethod(cl, mt, name, mp.toArray, resolveClass(sig, resultType)))
          }
          catch {
            case e: NoSuchMethodException => {
              // try companion object
              try {
                findClass(cl.getName + "$") map {
                  co =>
                    val mt = co.getMethod(name, paramTypes: _*)
                    val mp = for (((name, vt), index) <- params.zipWithIndex) yield MethodParameter(mt, index, name, vt)
                    CompanionMethod(co, mt, name, mp.toArray, resolveClass(sig, resultType))
                }
              }
              catch {
                case e: Exception =>
                  None
              }
            }
          }
        }

        val mSeq: Seq[ObjectMethod] = {
          val mOpt = targetMethodSymbol.map {
            s =>
              try {
                trace(s"method: $s")
                s match {
                  case (m: MethodSymbol, NullaryMethodType(resultType: TypeRefType)) => {
                    resolveMethod(cl, m.name, resultType, Seq.empty)
                  }
                  case (m: MethodSymbol, MethodType(resultType: TypeRefType, paramSymbols: Seq[_]))
                    if isAccessibleParams(paramSymbols.asInstanceOf[Seq[MethodSymbol]]) => {
                    val params = toAttribute(paramSymbols.asInstanceOf[Seq[MethodSymbol]], sig, cl)
                    resolveMethod(cl, m.name, resultType, params, resolveMethodArgTypes(params): _*)
                  }
                  case _ =>
                    None
                }
              }
              catch {
                case e: Exception => {
                  warn(s"error occurred when accessing method $s : $e")
                  warn(e)
                  None
                }
              }
          }
          mOpt.filter(_.isDefined) map (_.get)
        }
        mSeq
    }

    val p = parentMethodsOf(cl)
    trace(s"parent methods of ${cl.getSimpleName}: ${p.mkString(", ")}")
    ((methods getOrElse (Seq.empty)) ++ p).toArray
  }

  def parentMethodsOf(cl: Class[_]) = {
    def resolveImplOwner(m: ScMethod) = {
      m.owner
    }
    findParentSchemas(cl).flatMap(_.methods).map {
      m => m
    }
  }

  import scala.language.existentials

  def findClass(sig:ScalaSig, name: String, typeSignature: TypeRefType) : Class[_] = {
    trace(s"resolve class: $name $typeSignature")

    typeSignature match {
      case TypeRefType(t, AliasSymbol(symbolInfo), typeArgs) =>
        // alias to other type (e.g., type Id = Int)
        trace(s"Found type alias: ${symbolInfo}")
        sig.parseEntry(symbolInfo.info) match {
          case t@TypeRefType(prefix, symbol, args) =>
            findClass(sig, symbol.path, t)
          case other =>
            warn(s"Unknown alias type")
            classOf[Any]
        }
      case other =>
        name match {
          // Resolve classes of primitive types.
          // This special treatment is necessary because classes of primitive types, classOf[scala.Int] etc. are converted by
          // Scala compiler into Java primitive types (e.g., int, float). So classOf[Int] is represented as classOf[int] internally.
          case "scala.Boolean" => classOf[Boolean]
          case "scala.Byte" => classOf[Byte]
          case "scala.Short" => classOf[Short]
          case "scala.Char" => classOf[Char]
          case "scala.Int" => classOf[Int]
          case "scala.Float" => classOf[Float]
          case "scala.Long" => classOf[Long]
          case "scala.Double" => classOf[Double]
          case "scala.Predef.String" => classOf[String]
          // Map and Set type names are defined in Scala.Predef
          case "scala.Predef.Map" => classOf[Map[_, _]]
          case "scala.Predef.Set" => classOf[Set[_]]
          case "scala.Predef.Class" => classOf[Class[_]]
          case "scala.package.Throwable" => classOf[Throwable]
          case "scala.package.Exception" => classOf[Exception]
          case "scala.package.Error" => classOf[Error]
          case "scala.package.IndexedSeq" => classOf[IndexedSeq[_]]
          case "scala.package.Seq" => classOf[Seq[_]]
          case "scala.package.List" => classOf[List[_]]
          case "scala.package.Iterator" => classOf[Iterator[_]]
          case "scala.Any" => classOf[Any]
          case "scala.AnyRef" => classOf[AnyRef]
          case "scala.Array" => classOf[Array[_]]
          case "scala.Unit" => Unit.getClass
          case _ if typeSignature.symbol.isDeferred => classOf[AnyRef]
          case _ =>
            // Find the class using the context class loader
            val loader = Thread.currentThread().getContextClassLoader
            Try(loader.loadClass(name)) match {
              case Success(cl) =>
                cl
              case Failure(e) =>
                // When the class is defined in an object, its class name has suffix '$' like "xerial.silk.SomeTest$A"
                val parent = typeSignature.symbol.parent
                val anotherClassName = "%s$%s".format(if (parent.isDefined) parent.get.path else "", typeSignature.symbol.name)
                Try(loader.loadClass(anotherClassName)) match {
                  case Success(cl) => cl
                  case Failure(e) =>
                    warn(s"No corresponding class for ${name} is found", e)
                    classOf[Any]
                }
            }
        }
    }
  }

  def resolveClass(sig:ScalaSig, typeSignature: TypeRefType): ObjectType = {
    val name = typeSignature.symbol.path

    def resolveTypeArg: Seq[ObjectType] = typeSignature.typeArgs.collect {
      case x: TypeRefType if !(x.symbol.name.startsWith("_$")) => resolveClass(sig, x)
      case other => AnyRefType
    }

    def toObjectType(cl: Class[_]): ObjectType = {
      typeSignature match {
        case TypeRefType(prefix, symbol, typeArgs) if typeArgs.isEmpty =>
          ObjectType.of(cl)
        case _ =>
          val resolved = resolveTypeArg
          if(resolved.size == 1 && resolved(0).isInstanceOf[TaggedObjectType]) {
            resolved(0)
          }
          else {
            GenericType(cl, resolved)
          }
      }
    }

    val result = name match {
      case "scala.Array" =>
        // primitive type array
        val elementType = resolveTypeArg.head
        val arrayType = elementType match {
          case p: Primitive => p.arrayType
          case _ => Class.forName(s"[L${elementType.rawType.getName};")
        }
        ArrayType(arrayType, elementType)
      case "wvlet.obj.tag.package.$at$at" =>
        // Binding for tagged type
        trace(s"type signature: ${typeSignature.typeArgs}")
        /**
          * List(
          *    TypeRefType(
          *      ThisType(ClassSymbol(ServiceMixinExample, owner=wvlet.helix, flags=400, info=11 ,None)),
          *      ClassSymbol(Fruit, owner=9, flags=40, info=485 ,None),
          *      List()
          *    ),
          *    TypeRefType(
          *      ThisType(ClassSymbol(ServiceMixinExample, owner=wvlet.helix, flags=400, info=11 ,None)),
          *      ClassSymbol(Apple, owner=9, flags=2000880, info=530 ,None),
          *      List()
          *    )
          * )
          */
        val taggedType = resolveTypeArg
        TaggedObjectType(taggedType(0), taggedType(1))
      case _ =>
        toObjectType(findClass(sig, name, typeSignature))
    }


    trace(s"resolveClass: ${typeSignature} => ${result}")
    result
  }

}

