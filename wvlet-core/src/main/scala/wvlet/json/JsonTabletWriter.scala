package wvlet.json

import javax.lang.model

import wvlet.core.Output
import wvlet.core.tablet.{Tablet, TabletWriter}
import wvlet.core.time.TimeStamp
import xerial.lens.{Primitive, TypeConverter}

import scala.util.parsing.json.{JSONFormat, JSONObject}

/**
  *
  */

class JsonTabletWriter[Record](next:Output[String]) extends TabletWriter[Record] {

  private val record = Seq.newBuilder[String]

  def startRecord {
    record.clear()
  }

  def endRecord {
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
    record += (if(v) "true" else "false")
  }
  def writeString(v: String) = {
    record += s"\"${JSONFormat.quoteString(v)}\""
  }
  def writeBinary(v: Array[Byte], offset:Int, length:Int) {
    // TODO
  }
  def writeTimestamp(v : TimeStamp) {
    // TODO
  }
  def writeJson(v: String) {
    writeString(v)
  }
  def writeArray[A](v: Seq[A], elemType:Tablet.Type) {
    // TODO
    val arr = Seq.newBuilder[String]
    var i = 0
    for((x, i) <- v.zipWithIndex) {
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
  def writeMap[K, V](v: Map[K, V], keyType:Tablet.Type, valueType:Tablet.Type) {
    // TODO
  }

}

