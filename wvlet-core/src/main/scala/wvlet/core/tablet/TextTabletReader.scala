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
package wvlet.core.tablet
import wvlet.core.tablet.TabletReader.Line
import wvlet.core.time.TimeStamp


trait TextTabletReader extends TabletReader {

  def readLine[U](line:Line)(body: TabletReader => U)

}


/**
  *
  */
object TextTabletReader {

  class TSVTabletReader(schema:Schema) extends TextTabletReader {

    def readLine[U](line: Line)(body: TabletReader => U) {
      val cols = line.split("\t")



    }



    override def readLong: Long = ???
    // TODO type resolution of array and map elements
    override def readArray: Seq[_] = ???
    override def readNull: Unit = ???
    override def readTimestamp: TimeStamp = ???
    override def readMap: Map[_, _] = ???
    override def isNull: Boolean = ???
    override def readString: String = ???
    override def readDouble: Double = ???
    override def readBinary: Array[Byte] = ???
    override def readJson: String = ???

  }
}
