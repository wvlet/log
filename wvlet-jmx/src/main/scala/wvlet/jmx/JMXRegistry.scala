package wvlet.jmx

import java.lang.management.ManagementFactory
import javax.management._

import wvlet.log.LogSupport

import scala.util.{Failure, Try}

/**
  *
  */
trait JMXRegistry extends JMXMBeanServerService with LogSupport {

  private var registeredMBean = Set.empty[ObjectName]

  def register[A](obj: A) {
    val cl = obj.getClass
    val packageName = cl.getPackage.getName
    val name = s"${packageName}:name=${cl.getSimpleName}"
    register(name, obj)
  }

  def register[A](name: String, obj: A) {
    register(new ObjectName(name), obj)
  }

  def register[A](objectName: ObjectName, obj: A) {
    val mbean = JMXMBean.of(obj)
    mbeanServer.registerMBean(mbean, objectName)
    synchronized {
      registeredMBean += objectName
    }
    debug(s"Registered mbean: ${mbean}")
  }

  def unregister(name: String) {
    mbeanServer.unregisterMBean(new ObjectName(name))
  }

  def unregisterAll {
    synchronized {
      for (name <- registeredMBean) {
        Try(mbeanServer.unregisterMBean(name)) match {
          case Failure(e) =>
            error(e.getMessage, e)
          case _ =>
        }
      }
    }
  }
}
