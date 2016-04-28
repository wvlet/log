package wvlet.core.tablet

import wvlet.core.time.TimeStamp



trait TabletWriter[Record] {

  def tablet : Tablet
  def write(record:Record)

  def startRecord
  def endRecord

  def writeNull
  def writeLong(v: Long)
  def writeDouble(v: Double)
  def writeBoolean(v: Boolean)
  def writeString(v: String)
  def writeBinary(v: Array[Byte]) = writeBinary(v, 0, v.length)
  def writeBinary(v: Array[Byte], offset:Int, length:Int)
  def writeTimestamp(v : TimeStamp)
  def writeJson(v: String)
  def writeArray[A](v: Seq[A], elemType:Tablet.Type)
  def writeMap[K, V](v: Map[K, V], keyType:Tablet.Type, valueType:Tablet.Type)
}

