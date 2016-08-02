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

import java.io.File

import wvlet.log.LogSupport

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.ParSeq
import scala.reflect.ClassTag

/**
  * @since 2012/07/17
  */
object TypeUtil extends LogSupport {

  trait ClassToTag {
    def classToTag[T](cl:Class[T]) : ClassTag[T]
  }

  private implicit object DefaultClassTagConversion extends ClassToTag {
    def classToTag[T](cl: Class[T]) = ClassTag(cl)
  }

  //implicit def toClassTag[T](targetType: Class[T]): ClassTag[T] = ClassTag(targetType)

  @inline def cls[A](obj:A) : Class[_] = obj.asInstanceOf[AnyRef].getClass

  def isPrimitive[T](cl: Class[T]) = Primitive.isPrimitive(cl)

  def isArray[T](cl: Class[T]) = {
    cl.isArray || cl.getSimpleName == "Array"
  }

  /**
    * If the class has unapply(s:String) : T method in the companion object for instantiating class T, returns true.
    *
    * @param cl
    * @tparam T
    * @return
    */
  def hasStringUnapplyConstructor[T](cl:Class[T]) : Boolean = {
    companionObject(cl).map { co =>
      cls(co).getDeclaredMethods.find{ p =>
        def acceptString = {
          val t = p.getParameterTypes
          t.length == 1 && t(0) == classOf[String]
        }
        def returnOptionOfT = {
          val rt = p.getGenericReturnType
          val t = Reflect.getTypeParameters(rt)
          isOption(p.getReturnType) && t.length == 1 && t(0) == cl
        }

        p.getName == "unapply" && acceptString && returnOptionOfT
      }.isDefined
    }.getOrElse (false)
  }

  def isOption[T](cl: Class[T]): Boolean = {
    val name = cl.getSimpleName
    // Option None is an object ($)
    name == "Option" || name == "Some" || name == "None$"
  }

  def isBuffer[T](cl: Class[T]) = {
    classOf[mutable.Buffer[_]].isAssignableFrom(cl)
  }

  def isSeq[T](cl: Class[T]) = {
    classOf[Seq[_]].isAssignableFrom(cl)
  }

  def isParSeq[T](cl: Class[T]) = {
    classOf[ParSeq[_]].isAssignableFrom(cl)
  }

  def isIndexedSeq[T](cl: Class[T]) = {
    classOf[IndexedSeq[_]].isAssignableFrom(cl) || isArray(cl)
  }


  def isMap[T](cl: Class[T]) = {
    classOf[Map[_, _]].isAssignableFrom(cl)
  }

  def isSet[T](cl: Class[T]) = {
    classOf[Set[_]].isAssignableFrom(cl)
  }

  def isTuple[T](cl: Class[T]) = {
    classOf[Product].isAssignableFrom(cl) && cl.getName.startsWith("Tuple")
  }

  def isList[T](cl:Class[T]) ={
    classOf[List[_]].isAssignableFrom(cl)
  }

  def isEither[T](cl: Class[T]) = {
    classOf[Either[_, _]].isAssignableFrom(cl)
  }


  def isTraversable[T](cl: Class[T]) = classOf[Traversable[_]].isAssignableFrom(cl)

  def isTraversableOnce[T](cl: Class[T]) = classOf[TraversableOnce[_]].isAssignableFrom(cl)

  def toBuffer[A](input: Array[A]): collection.mutable.Buffer[A] = {
    input.toBuffer[A]
  }


  /**
    * Convert immutable collections or arrays to a mutable buffer
    *
    * @param input
    * @param valueType
    */
  def toBuffer(input: Any, valueType: ObjectType): collection.mutable.Buffer[_] = {

    def err = throw new IllegalArgumentException("cannot convert to ArrayBuffer: %s".format(valueType))

    if (!canBuildFromBuffer(valueType.rawType))
      err

    val cl: Class[_] = cls(input)
    if (isArray(cl)) {
      val a = input.asInstanceOf[Array[_]]
      a.toBuffer
    }
    else if (isTraversableOnce(cl) && valueType.isGenericType) {
      val gt = valueType.asInstanceOf[GenericType]
      val e = gt.genericTypes(0).rawType
      type E = e.type
      val l = input.asInstanceOf[TraversableOnce[E]]
      val b = new ArrayBuffer[E]
      l.foreach(b += _)
      b
    }
    else
      err
  }


  def elementType[T](cl: Class[T]) = {
    cl.getComponentType
  }

  def companionObject[A](cl: Class[A]): Option[Any] = {
    try {
      import scala.language.existentials

      val clName = cl.getName
      val companionCls = if(clName.endsWith("$")) cl else Class.forName(clName + "$")
      val module = companionCls.getField("MODULE$")
      val companionObj = module.get(null)
      Some(companionObj)
    }
    catch {
      case e : Throwable => {
        //warn(s"no companion object is found for $cl)
        //warn(e)
        None
      }
    }
  }

  def hasDefaultConstructor[A](cl: Class[A]) = {
    cl.getConstructors.find(x => x.getParameterTypes.length == 0).isDefined
  }

  def canInstantiate[A](cl: Class[A]): Boolean = {
    if (isPrimitive(cl) || hasDefaultConstructor(cl))
      return true

    ObjectSchema(cl).findConstructor.map { cc =>
      cc.params.map(_.valueType.rawType).forall{ t =>
        t != cl && canInstantiate(t)
      }
    } getOrElse(false)
//
//    val fields = cl.getDeclaredFields
//    debug(s"fields of $cl: ${fields.mkString(", ")}")
//    val c = cl.getConstructors().find {
//      x =>
//        val p = x.getParameterTypes

//        debug(s"parameter types: ${p.mkString(", ")}")
//        if (p.length != fields.length)
//          false
//        else
//          fields.zip(p).forall(e => e._1.getType == e._2)
//    }
//
//    c.isDefined
  }

  def canBuildFromBuffer[T](cl: Class[T]) = isArray(cl) || isSeq(cl) || isMap(cl) || isSet(cl)
  def canBuildFromString[T](cl: Class[T]) = isPrimitive(cl) || hasStringUnapplyConstructor(cl)

  def zero[A](cl:Class[A], param: ObjectType) : A = {
    param match {
      case ArrayType(cl, elemType) => ClassTag(elemType.rawType).newArray(0).asInstanceOf[A]
      case _ => zero[A](cl)
    }
  }

  def zero[A](cl: Class[A]): A = {
    if (isPrimitive(cl)) {
      val v: Any = Primitive(cl) match {
        case Primitive.Boolean => true
        case Primitive.Int => 0
        case Primitive.Float => 0f
        case Primitive.Double => 0.0
        case Primitive.Long => 0L
        case Primitive.Short => 0.toShort
        case Primitive.Byte => 0.toByte
        case Primitive.Char => 0.toChar
      }
      v.asInstanceOf[A]
    }
    else if (cl == classOf[Nothing] || cl == classOf[AnyRef] || cl == classOf[Any]) {
      null.asInstanceOf[A]
    }
    else if (TextType.isTextType(cl)) {
      val v: Any = TextType(cl) match {
        case TextType.String => ""
        case TextType.Date => new java.util.Date(0)
        case TextType.File => new File("")
      }
      v.asInstanceOf[A]
    }
    else if (isArray(cl)) {
      ClassTag(elementType(cl)).newArray(0).asInstanceOf[A]
    }
    else if (isMap(cl)) {
      Map.empty.asInstanceOf[A]
    }
    else if (isSeq(cl)) {
      Seq.empty.asInstanceOf[A]
    }
    else if (isSet(cl)) {
      Set.empty.asInstanceOf[A]
    }
    else if (isOption(cl)) {
      None.asInstanceOf[A]
    }
    else if (canInstantiate(cl)) {
      newInstance(cl).asInstanceOf[A]
    }
    else if (isTuple(cl)) {
      val c = cl.getDeclaredConstructors()(0)
      val elementType = cl.getTypeParameters
      val arity = elementType.length
      val args = for (i <- 1 to arity) yield {
        val m = cl.getMethod("_%d".format(i))
        zero(m.getReturnType).asInstanceOf[AnyRef]
      }
      newInstance(cl, args.toSeq)
    }
    else
      null.asInstanceOf[A]
  }


  def defaultConstructorParameters[A](cl: Class[A]): Seq[AnyRef] = {
    val d = for(p <- ObjectSchema(cl).constructor.params) yield {
      val v = p.getDefaultValue getOrElse zero(p.rawType, p.valueType)
      v.asInstanceOf[AnyRef]
    }

    d
  }

  def newInstance[A, B <: AnyRef](cl: Class[A], args: Seq[B]): A = {
    val cc = cl.getConstructors()(0)
    val obj = cc.newInstance(args: _*)
    obj.asInstanceOf[A]
  }

  def newInstance[A](cl: Class[A]): A = {
    def createDefaultInstance: A = {
      trace(s"Creating a default instance of $cl")
      val hasOuter = cl.getDeclaredFields.exists(x => x.getName == "$outer")
      if (hasOuter)
        throw new IllegalArgumentException("Cannot instantiate the inner class %s. Use classes defined globally or in companion objects".format(cl.getName))
      val paramArgs = defaultConstructorParameters(cl)
      val cc = cl.getConstructors()(0)
      val obj = cc.newInstance(paramArgs: _*)
      trace(s"create an instance of $cl, args:[${paramArgs.mkString(", ")}]")
      obj.asInstanceOf[A]
    }

    try {
      val c = cl.getConstructor()
      cl.newInstance.asInstanceOf[A]
    }
    catch {
      case e: NoSuchMethodException => createDefaultInstance
    }
  }

}