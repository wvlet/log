package wvlet.helix


import java.util.concurrent.ConcurrentHashMap

import wvlet.helix.HelixException.CYCLIC_DEPENDENCY
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, ObjectType, TypeUtil}

import scala.reflect.ClassTag
import scala.language.experimental.macros
import scala.util.{Failure, Try}

object Helix {

  sealed trait Binding {
    def from :  ObjectType
  }
  case class ClassBinding(from:ObjectType, to:ObjectType) extends Binding
  case class InstanceBinding(from:ObjectType, to:Any) extends Binding
  case class SingletonBinding(from:ObjectType, to:ObjectType, isEager:Boolean) extends Binding

}

import Helix._
import com.softwaremill.tagging._

/**
  *
  */
class Helix extends LogSupport {

  private val binding = Seq.newBuilder[Binding]
  private val listener = Seq.newBuilder[ContextListener]

  def bind[A](implicit a:ClassTag[A]) : Bind = {
    info(s"Bind ${a}")
    val b = new Bind(this, ObjectType(a.runtimeClass))
    b
  }

  def addListner[A](l:ContextListener) : Helix = {
    listener += l
    this
  }


  def newContext : Context = {
    new ContextImpl(binding.result, listener.result())
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

  def toSingletonOf[B](implicit ev:ClassTag[B]) {
    val to = ObjectType(ev.runtimeClass)
    if(from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(SingletonBinding(from, to, false))
    }
  }

  def toEagerSingletonOf[B](implicit ev:ClassTag[B]) {
    val to = ObjectType(ev.runtimeClass)
    if(from == to) {
      warn(s"Binding to the same type will be ignored: ${from.name}")
    }
    else {
      h.addBinding(SingletonBinding(from, to, true))
    }
  }

  def toInstance(any:Any) {
    h.addBinding(InstanceBinding(from, any))
  }

  def toSingleton {
    h.addBinding(SingletonBinding(from, from, false))
  }

  def toEagerSingleton {
    h.addBinding(SingletonBinding(from, from, true))
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

  def build[A:ClassTag] : A = macro HelixMacros.weaveImpl[A]

}

trait ContextListener {

  def afterInjection(t:ObjectType, injectee:Any)
}


private[helix] class ContextImpl(binding:Seq[Binding], listener:Seq[ContextListener]) extends wvlet.helix.Context with LogSupport {

  import scala.collection.JavaConversions._
  private lazy val singletonHolder : collection.mutable.Map[ObjectType, AnyRef] = new ConcurrentHashMap[ObjectType, AnyRef]()

  // Initialize eager singleton
  binding.collect {
    case s@SingletonBinding(from, to, eager) if eager =>
      singletonHolder.getOrElseUpdate(to, buildInstance(to, Set(to)))
    case InstanceBinding(from, obj) =>
      reportToListener(from, obj)
  }

  /**
    * Creates an instance of the given type A.
    *
    * @return object
    */
  def get[A](implicit ev:ClassTag[A]): A = {
    info(s"Get ${ev}")
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
      case SingletonBinding(from, to, eager) =>
        info(s"Find a singleton for ${to}")
        singletonHolder.getOrElseUpdate(to, buildInstance(to, seen + t + to))
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
    val obj = schema.constructor.newInstance(args).asInstanceOf[AnyRef]
    reportToListener(t, obj)
    obj
  }

  private def reportToListener(t:ObjectType, obj:Any) {
    listener.map(l => Try(l.afterInjection(t, obj))).collect {
      case Failure(e) =>
        error(s"Error in ContextListher", e)
        throw e
    }
  }

}




