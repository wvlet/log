package wvlet.helix


import java.util.concurrent.ConcurrentHashMap

import wvlet.helix.HelixException.CYCLIC_DEPENDENCY
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType, TypeUtil}

import scala.reflect.ClassTag
import scala.language.experimental.macros

object Helix {

  sealed trait Binding {
    def from :  ObjectType
  }
  case class ClassBinding(from:ObjectType, to:ObjectType) extends Binding
  case class InstanceBinding(from:ObjectType, to:Any) extends Binding
  case class SingletonBinding(from:ObjectType, isEager:Boolean) extends Binding

}

import Helix._

/**
  *
  */
class Helix extends LogSupport {

  private val binding = Seq.newBuilder[Binding]

  def bind[A](implicit a:ClassTag[A]) : Bind = {
    new Bind(this, ObjectType(a.runtimeClass))
  }

  def bind[A](obj:A)(implicit a:ClassTag[A]) : Helix = {
    binding += InstanceBinding(ObjectType(a.runtimeClass), obj)
    this
  }

  def newContext : Context = {
    new ContextImpl(binding.result)
  }

  def addBinding(b:Binding) : Helix = {
    binding += b
    this
  }
}


class Bind(h:Helix, from:ObjectType) extends LogSupport {

  def to[B](implicit ev:ClassTag[B]) {
    val to = ObjectType(ev.runtimeClass)
    if(from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(ClassBinding(from, to))
    }
  }

  def toInstance(any:Any) {
    h.addBinding(InstanceBinding(from, any))
  }

  def asSingleton {
    h.addBinding(SingletonBinding(from, false))
  }

  def asEagerSingleton {
    h.addBinding(SingletonBinding(from, true))
  }
}

/**
  * Context tracks the dependencies of objects and use them to instantiate objects
  */
trait Context {

  /**
    * Creates an instance of the given type A.
    *
    * @tparam A
    * @return object
    */
  def get[A:ClassTag] : A

  def weave[A:ClassTag] : A = macro HelixMacros.weaveImpl[A]

}

trait ContextListener {

  def afterInjection(t:ObjectType, injectee:AnyRef)
}


private[helix] class ContextImpl(binding:Seq[Binding]) extends wvlet.helix.Context with LogSupport {

  import scala.collection.JavaConversions._
  private lazy val singletonHolder : collection.mutable.Map[ObjectType, AnyRef] = new ConcurrentHashMap[ObjectType, AnyRef]()

  // Initialize eager singleton
  binding.collect {
    case s@SingletonBinding(from, eager) if eager =>
      singletonHolder.getOrElseUpdate(from, buildInstance(from, Set(from)))
  }

  /**
    * Creates an instance of the given type A.
    *
    * @return object
    */
  def get[A](implicit ev:ClassTag[A]): A = {
    val cl = ev.runtimeClass

    newInstance(cl).asInstanceOf[A]
  }

  private def newInstance(cl:Class[_]) : AnyRef = {
    newInstance(ObjectType(cl), Set.empty)
  }

  private def newInstance(t:ObjectType, seen:Set[ObjectType]) : AnyRef = {
    info(s"Search bindings for ${t}")
    if(seen.contains(t)) {
      error(s"Found cyclic dependencies: ${seen}")
      throw new HelixException(CYCLIC_DEPENDENCY(seen))
    }
    val obj = binding.find(_.from == t).map {
      case ClassBinding(from, to) =>
        newInstance(to, seen + from)
      case InstanceBinding(from, obj) =>
        info(s"Pre-defined instance is found for ${from}")
        obj
      case SingletonBinding(from, eager) =>
        info(s"Find a singleton for ${from}")
        singletonHolder.getOrElseUpdate(from, buildInstance(from, seen + t))
    }
    .getOrElse {
      buildInstance(t, seen + t)
    }
    obj.asInstanceOf[AnyRef]
  }

  private def buildInstance(t:ObjectType, seen:Set[ObjectType]) : AnyRef = {
    val schema = ObjectSchema(t.rawType)
    val args = for (p <- schema.constructor.params) yield {
      newInstance(p.valueType, seen)
    }
    info(s"Build a new instance for ${t}")
    schema.constructor.newInstance(args).asInstanceOf[AnyRef]
  }

}




