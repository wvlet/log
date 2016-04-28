package wvlet.core.tablet

import wvlet.core.{Output, Producer}
import wvlet.core.time.TimeStamp
import xerial.lens.{Primitive, TypeConverter}

import scala.util.parsing.json.JSONFormat

object TextTabletWriter {

  trait RecordFormatter {
    def sanitize(s:String) : String = s
    def format(record:Seq[String]) : String
  }

  object JSONRecordFormatter extends RecordFormatter {
    override def sanitize(s: String): String = s"\"${JSONFormat.quoteString(s)}\""
    override def format(record: Seq[String]): String = {
      s"[${record.mkString(", ")}]"
    }
  }

  object TSVRecordFormatter extends RecordFormatter {
    override def sanitize(s:String) : String = {
      s.map {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case c => c
      }.mkString
    }

    override def format(record: Seq[String]): String = {
      record.mkString("\t")
    }
  }

  object CSVRecordFormatter extends RecordFormatter {
    override def sanitize(s:String) : String = {
      var hasComma = false
      val sanitized = s.map {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case ',' =>
          hasComma = true
          ','
        case c => c
      }.mkString
      if(hasComma) s"\"${sanitized}\"" else sanitized
    }

    override def format(record: Seq[String]): String = {
      record.mkString(",")
    }
  }

}

import TextTabletWriter._

/**
  *
  */
class TextTabletWriter(formatter:RecordFormatter, next:Output[String]) extends TabletWriter with Producer[String] {

  protected val record = Seq.newBuilder[String]

  def writeRecord(body: => Unit) {
    record.clear()
    body
    val recordText = formatter.format(record.result())
    next.onNext(recordText)
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
    record += formatter.sanitize(v)
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


class JSONTabletWriter(next: Output[String]) extends TextTabletWriter(JSONRecordFormatter, next)
class CSVTabletWriterr(next: Output[String]) extends TextTabletWriter(CSVRecordFormatter, next)
class TSVTabletWriter(next: Output[String]) extends TextTabletWriter(TSVRecordFormatter, next)



