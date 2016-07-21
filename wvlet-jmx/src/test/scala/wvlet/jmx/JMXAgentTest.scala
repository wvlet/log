package wvlet.jmx

import javax.management.remote.{JMXConnectorFactory, JMXServiceURL}

import wvlet.test.WvletSpec

/**
  *
  */
class JMXAgentTest extends WvletSpec {

  "JMXAgent" should {
    "start jmx registry" in {
      val agent = new JMXAgent(new JMXConfig())
      val url = new JMXServiceURL(agent.serviceUrl)
      val connector = JMXConnectorFactory.connect(url)
      connector.connect()

      val connection = connector.getMBeanServerConnection()
      connection.getMBeanCount.toInt shouldBe > (0)
    }

  }
}
