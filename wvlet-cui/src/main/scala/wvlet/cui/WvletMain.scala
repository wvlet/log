/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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