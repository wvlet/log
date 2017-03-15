package wvlet.log

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec, _}
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.io.Timer

trait JVMSpec extends WordSpec
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with Timer
  with LogSupport {

  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  override protected def beforeAll(): Unit = {
    // Run LogLevel scanner (log-test.properties or log.properties in classpath) every 1 minute
    LogLevelScanner.scheduleLogLevelScan
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    LogLevelScanner.stopScheduledLogLevelScan
    super.afterAll()
  }
}