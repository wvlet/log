package wvlet.obj

import java.util.Locale

import wvlet.log.LogSupport

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

//--------------------------------------
//
// ObjectBuilder.scala
// Since: 2012/01/25 12:41
//
//--------------------------------------

/**
  *
  *
  */
object ObjectBuilder extends LogSupport {

  def apply[A](cl: Class[A]): ObjectBuilder[A] = {
    //if (!TypeUtil.canInstantiate(cl))
//      throw new IllegalArgumentException("Cannot instantiate class " + cl)
    new SimpleObjectBuilder(cl)
  }

  sealed trait BuilderElement
  case class Holder[A](holder: ObjectBuilder[A]) extends BuilderElement
  case class Value(value: Any) extends BuilderElement
  case class ArrayHolder(holder: mutable.ArrayBuffer[Any]) extends BuilderElement

  trait ParameterNameFormatter
  case object CanonicalNameFormatter extends ParameterNameFormatter {
    def format(name:String) : String = {
      name.toLowerCase(Locale.US).replaceAll("[ _\\.-]", "")
    }
  }

}

trait GenericBuilder {

  def set(path: String, value: Any): Unit = set(Path(path), value)
  def set(path: Path, value: Any): Unit

  def get(name: String): Option[Any]
}

/**
  * Generic object builder
  *
  * @author leo
  */
trait ObjectBuilder[A] extends GenericBuilder {

  def build: A

}

trait StandardBuilder[ParamType <: Parameter] extends GenericBuilder with LogSupport {

  import ObjectBuilder._
  import TypeUtil._

  protected val holder = collection.mutable.Map.empty[String, BuilderElement]

  protected def findParameter(name: String): Option[ParamType]
  protected def getParameterTypeOf(name: String) = findParameter(name).get.valueType

  protected def defaultValues: collection.immutable.Map[String, Any]

  // set the default values of the object
  for ((name, value) <- defaultValues) {
    val v: BuilderElement = findParameter(name).map {
      case p if TypeUtil.canBuildFromBuffer(p.rawType) => Value(value)
      case p if canBuildFromStringValue(p.valueType) => Value(value)
      case p => {
        // nested object
        // TODO handling of recursive objects
        val b = ObjectBuilder(p.rawType)
        val schema = ObjectSchema(p.rawType)
        for (p <- schema.constructor.params) {
          b.set(p.canonicalName, p.get(value))
        }
        Holder(b)
      }
    } getOrElse Value(value)

    holder += name -> v
  }

  private def canBuildFromStringValue(t: ObjectType): Boolean = {
    import scala.language.existentials

    if (TextType.isTextType(t.rawType) || canBuildFromString(t.rawType)) {
      true
    }
    else {
      t match {
        case o: OptionType[_] => canBuildFromStringValue(o.elementType)
        case _ => false
      }
    }
  }

  def set(path: Path, value: Any) {
    if (path.isEmpty) {
      // do nothing
      return
    }
    val name = CanonicalNameFormatter.format(path.head)
    val p = findParameter(name)
    if (p.isEmpty) {
      error(s"no parameter is found for path $path")
      return
    }

    trace(s"set path $path : $value")

    if (path.isLeaf) {
      val valueType = p.get.valueType
      trace(s"update value holder name:$name, valueType:$valueType (isArray:${TypeUtil.isArray(valueType.rawType)}) with value:$value")
      if (canBuildFromBuffer(valueType.rawType)) {
        val t = valueType.asInstanceOf[GenericType]
        val gt = t.genericTypes(0)

        holder.get(name) match {
          case Some(Value(v)) =>
            // remove the default value
            holder.remove(name)
          case _ => // do nothing
        }
        val arr = holder.getOrElseUpdate(name, ArrayHolder(new ArrayBuffer[Any])).asInstanceOf[ArrayHolder]
        TypeConverter.convert(value, gt) map {arr.holder += _}
      }
      else if (canBuildFromStringValue(valueType)) {
        TypeConverter.convert(value, valueType).map { v =>
          holder += name -> Value(v)
        }
      }
      else {
        error(s"failed to set $value to path $path")
      }
    }
    else {
      // nested object
      val paramName = CanonicalNameFormatter.format(path.head)
      val h = holder.getOrElseUpdate(paramName, Holder(ObjectBuilder(p.get.valueType.rawType)))
      h match {
        case Holder(b) => b.set(path.tailPath, value)
        case other =>
          // overwrite the existing holder
          throw new IllegalStateException("invalid path:%s, value:%s, holder:%s".format(path, value, other))
      }
    }
  }

  def get(name: String): Option[Any] = {
    val paramName = CanonicalNameFormatter.format(name)
    holder.get(paramName) flatMap {
      case Holder(h) => Some(h.build)
      case Value(v) => Some(v)
      case ArrayHolder(h) => {
        val p = getParameterTypeOf(paramName)
        debug(s"convert array holder:$h into $p")
        TypeConverter.convert(h, p)
      }
    }
  }
}

class SimpleObjectBuilder[A](cl: Class[A]) extends ObjectBuilder[A] with StandardBuilder[Parameter] with LogSupport {

  private lazy val schema = ObjectSchema(cl)

  protected def findParameter(name: String) = {
    assert(schema != null)
    schema.findParameter(name)
  }
  protected def defaultValues = {
    // collect default values of the object
    val schema = ObjectSchema(cl)
    val prop = Map.newBuilder[String, Any]

    // get the default values of the constructor
    for (c <- schema.findConstructor; p <- c.params; v <- p.getDefaultValue) {
      trace(s"set default parameter $p to $v")
      prop += p.canonicalName -> v
    }
    val r = prop.result
    trace(s"class ${cl.getSimpleName}. values to set: $r")
    r
  }

  def build: A = {
    trace(s"holder contents: $holder")
    val cc = schema.constructor
    // Prepare constructor args
    val args = (for (p <- cc.params) yield {
      (get(p.canonicalName) getOrElse TypeUtil.zero(p.rawType, p.valueType)).asInstanceOf[AnyRef]
    })
    trace(s"cc:$cc, args:${args.mkString(", ")} (size:${args.length})")
    cc.newInstance(args).asInstanceOf[A]
  }

}

