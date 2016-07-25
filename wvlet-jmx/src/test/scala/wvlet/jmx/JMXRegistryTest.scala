package wvlet.jmx

import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}
import javax.management.{MBeanInfo, ObjectName}

import wvlet.core.io.IOUtil
import wvlet.test.WvletSpec

import scala.util.Random

@JMX(description = "A example MBean object")
class SampleMBean {
  @JMX(description = "free memory size")
  def freeMemory: Long = {
    Runtime.getRuntime.freeMemory()
  }
}

case class FieldMBean(@JMX a:Int, @JMX b:String)

class NestedMBean {
  @JMX(description = "nested stat")
  def stat: Stat = {
    new Stat(Random.nextInt(10), "nested JMX bean")
  }
}

case class Stat(@JMX count:Int, @JMX state:String)

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

      val a = agent.getMBeanAttribute("wvlet.jmx:name=SampleMBean", "freeMemory")
      info(a)
    }

    "support class field" in {
      val f = new FieldMBean(1, "apple")
      agent.register(f)
      val m = agent.getMBeanInfo("wvlet.jmx:name=FieldMBean")
      info(m)

      agent.getMBeanAttribute("wvlet.jmx:name=FieldMBean", "a") shouldBe 1
      agent.getMBeanAttribute("wvlet.jmx:name=FieldMBean", "b") shouldBe "apple"
    }

    "handle nested JMX MBean" in {
      val n = new NestedMBean
      agent.register(n)
      val m = agent.getMBeanInfo("wvlet.jmx:name=NestedMBean")
      info(m)

      agent.getMBeanAttribute("wvlet.jmx:name=NestedMBean", "stat.count").toString.toInt should be <= 10
      agent.getMBeanAttribute("wvlet.jmx:name=NestedMBean", "stat.state") shouldBe ("nested JMX bean")
    }

  }
}
