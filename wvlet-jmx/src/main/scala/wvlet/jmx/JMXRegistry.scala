package wvlet.jmx

import java.lang.management.ManagementFactory
import javax.management._

import wvlet.log.LogSupport


/**
  *
  */
object JMXRegistry extends LogSupport {

  private val mbeanServer = ManagementFactory.getPlatformMBeanServer

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
    info(s"Registering mbean: ${mbean}")
    mbeanServer.registerMBean(mbean, objectName)
  }

  def unregister(name:String) {
    mbeanServer.unregisterMBean(new ObjectName(name))
  }

}
