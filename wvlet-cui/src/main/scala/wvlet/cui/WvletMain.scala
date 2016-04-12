package wvlet.cui

import java.io.File

import xerial.core.log.Logger
import xerial.lens.cui.{DefaultCommand, Launcher, command, option}

/**
  *
  */
object WvletMain {

  def main(args: Array[String]) {
    val l = Launcher.of[WvletMain]
    l.execute(args)
  }

  def getVersion = {
    sys.props.getOrElse("prog.version", "unknown")
  }

  private def getVersionFile = {
    val home = System.getProperty("prog.home")
    new File(home, "VERSION")
  }

  def getBuildTime: Option[Long] = {
    val versionFile = getVersionFile
    if (versionFile.exists()) {
      Some(versionFile.lastModified())
    }
    else {
      None
    }
  }
}

class WvletMain(@option(prefix = "-h,--help", description = "display help messages", isHelp = true)
                displayHelp: Boolean) extends Logger with DefaultCommand {

  override def default: Unit = {
    version
    info(s"Type wv --help to show the list of sub commands")
  }

  @command(description = "show version")
  def version = {
    info(s"wvlet: version ${WvletMain.getVersion}")
  }

}