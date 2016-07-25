package wvlet.jmx

import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}
import javax.management.{MBeanInfo, ObjectName}

import wvlet.core.io.IOUtil
import wvlet.test.WvletSpec

@JMX(description = "A example MBean object")
class SampleMBean {

  @JMX(description = "number of threads")
  def numThreads: Int = {
    Runtime.getRuntime.availableProcessors()
  }

}

/**
  *
  */
class JMXRegistryTest extends WvletSpec {

  val agent = new JMXAgent(new JMXConfig())

  override def afterAll {
    JMXRegistry.unregister("wvlet.jmx:name=SampleMBean")
  }
  def getMBeanInfo(mbeanName: String): MBeanInfo = {
    import IOUtil._
    withResource(JMXConnectorFactory.connect(new JMXServiceURL(agent.serviceUrl))) { connector =>
      val connection = connector.getMBeanServerConnection()
      connection.getMBeanInfo(new ObjectName(mbeanName))
    }
  }

  def getMBeanAttr(mbeanName: String, attrName:String): Any = {
    import IOUtil._
    withResource(JMXConnectorFactory.connect(new JMXServiceURL(agent.serviceUrl))) { connector =>
      val connection = connector.getMBeanServerConnection()
      connection.getAttribute(new ObjectName(mbeanName), attrName)
    }
  }
  "JMXRegistry" should {
    "register a new mbean" in {

      val b = new SampleMBean
      JMXRegistry.register(b)
      val m = getMBeanInfo("wvlet.jmx:name=SampleMBean")
      info(m)

      val a = getMBeanAttr("wvlet.jmx:name=SampleMBean", "numThreads")
      info(a)
    }

  }
}
