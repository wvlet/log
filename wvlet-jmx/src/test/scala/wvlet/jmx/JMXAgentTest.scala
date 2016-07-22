package wvlet.jmx

import javax.management.ObjectName
import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}

import com.sun.org.glassfish.gmbal.AMXClient
import wvlet.test.WvletSpec

/**
  *
  */
class JMXAgentTest extends WvletSpec {

  "JMXAgent" should {
    "find jmx registry" in {
      val agent = new JMXAgent(new JMXConfig())
      val url = new JMXServiceURL(agent.serviceUrl)
      val connector = JMXConnectorFactory.connect(url)
      connector.connect()

      val connection = connector.getMBeanServerConnection()
      connection.getMBeanCount.toInt shouldBe > (0)
      val m = connection.getMBeanInfo(new ObjectName("java.lang:type=OperatingSystem"))
      m shouldNot be (null)
      info(m)
    }
  }
}
