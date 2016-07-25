package wvlet.jmx

import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}
import javax.management.{MBeanInfo, ObjectName}

import wvlet.core.io.IOUtil
import wvlet.test.WvletSpec

@JMX(description = "A example MBean object")
class SampleMBean {

  @JMX(description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }

}

/**
  *
  */
class JMXRegistryTest extends WvletSpec {

  val agent = new JMXAgent(new JMXConfig())

  override def afterAll {
    agent.unregisterAll
  }

  "JMXRegistry" should {
    "register a new mbean" in {
      val b = new SampleMBean
      agent.register(b)
      val m = agent.getMBeanInfo("wvlet.jmx:name=SampleMBean")
      info(m)

      val a = agent.getMBeanAttr("wvlet.jmx:name=SampleMBean", "freeMemory")
      info(a)
    }

  }
}
