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

import wvlet.core.time.TimeStamp

object TabletReader {
  type Line = String
}

/**
  *
  */
trait TabletReader {

  def isNull : Boolean
  def readNull : Unit
  def readLong : Long
  def readDouble : Double
  def readString : String
  def readBinary : Array[Byte]
  def readTimestamp : TimeStamp
  def readJson : String

  // TODO type resolution of array and map elements
  def readArray: Seq[_]
  def readMap: Map[_, _]
}

