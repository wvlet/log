package wvlet.core.tablet

import wvlet.core.time.TimeStamp

/**
  *
  */
trait TabletReader {
  def writeRecord(body: => Unit)

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

