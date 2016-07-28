package wvlet.helix


import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, TypeUtil}

import scala.reflect.ClassTag

object Helix {

  sealed trait Binding {
    def from :  Class[_]
  }
  case class ClassBinding(from:Class[_], to:Class[_]) extends Binding
  case class InstanceBinding(from:Class[_], to:Any) extends Binding

}

import Helix._

/**
  *
  */
class Helix extends LogSupport {

  private val binding = Seq.newBuilder[Binding]

  def bind[A](implicit a:ClassTag[A]) : Bind = {
    new Bind(this, a.runtimeClass)
  }

  def bind[A](obj:A)(implicit a:ClassTag[A]) : Helix = {
    binding += InstanceBinding(a.runtimeClass, obj)
    this
  }

  def getContext : Context = {
    new InternalContext(binding.result)
  }

  def addBinding(b:Binding) : Helix = {
    binding += b
    this
  }
}


class Bind(h:Helix, from:Class[_]) {

  def to[B](implicit ev:ClassTag[B]) {
    h.addBinding(ClassBinding(from, ev.runtimeClass))
  }

  def toInstance(any:Any) {
    h.addBinding(InstanceBinding(from, any))
  }
}




private[helix] class InternalContext(binding:Seq[Binding]) extends wvlet.helix.Context with LogSupport {
  /**
    * Creates an instance of the given type A.
    *
    * @return object
    */
  def get[A](implicit ev:ClassTag[A]): A = {
    val cl = ev.runtimeClass
    info(s"Get ${cl.getName}")

    newInstance(cl).asInstanceOf[A]
  }

  private def newInstance(cl:Class[_]) : AnyRef = {
    val obj = binding.find(_.from == cl).map {
      case ClassBinding(from, to) =>
        newInstance(to)
      case InstanceBinding(from, obj) =>
        obj
    }
    .getOrElse {
      val schema = ObjectSchema(cl)
      val args = for (p <- schema.constructor.params) yield {
        newInstance(p.valueType.rawType)
      }
      schema.constructor.newInstance(args).asInstanceOf[AnyRef]
    }
    obj.asInstanceOf[AnyRef]
  }

}


