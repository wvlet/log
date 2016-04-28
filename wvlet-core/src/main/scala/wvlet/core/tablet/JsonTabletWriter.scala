package wvlet.core.tablet

import wvlet.core.time.TimeStamp
import wvlet.core.{Output, Producer}
import xerial.lens.{Primitive, TypeConverter}

import scala.util.parsing.json.JSONFormat

/**
  *
  */

class JsonTabletWriter(next: Output[String])
  extends TabletWriter
    with Producer[String] {

  private val record = Seq.newBuilder[String]

  def writeRecord(body: => Unit) {
    record.clear()
    body
    val arr = record.result()
    val json = s"[${arr.mkString(",")}]"
    next.onNext(json)
  }

  def writeNull = {
    record += "null"
  }
  def writeLong(v: Long) = {
    record += v.toString
  }
  def writeDouble(v: Double) = {
    record += v.toString
  }
  def writeBoolean(v: Boolean) = {
    record += (if (v) "true" else "false")
  }
  def writeString(v: String) = {
    record += s"\"${JSONFormat.quoteString(v)}\""
  }
  def writeBinary(v: Array[Byte], offset: Int, length: Int) {
    // TODO
  }
  def writeTimestamp(v: TimeStamp) {
    // TODO
  }
  def writeJson(v: String) {
    writeString(v)
  }
  def writeArray[A](v: Seq[A], elemType: Tablet.Type) {
    // TODO
    val arr = Seq.newBuilder[String]
    var i = 0
    for ((x, i) <- v.zipWithIndex) {
      elemType match {
        case Tablet.INTEGER =>
          TypeConverter.convertToPrimitive(x, Primitive.Long).map(writeLong(_))
        case Tablet.INTEGER =>
          TypeConverter.convertToPrimitive(x, Primitive.Long).map(writeLong(_))
        case Tablet.INTEGER =>
          TypeConverter.convertToPrimitive(x, Primitive.Long).map(writeLong(_))
        case Tablet.INTEGER =>
          TypeConverter.convertToPrimitive(x, Primitive.Long).map(writeLong(_))

      }
    }
    record += s"[${arr.result.mkString(",")}]"
  }
  def writeMap[K, V](v: Map[K, V], keyType: Tablet.Type, valueType: Tablet.Type) {
    // TODO
  }

}

