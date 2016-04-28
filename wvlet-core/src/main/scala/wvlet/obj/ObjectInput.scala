package wvlet.obj

import wvlet.core.{Wvlet, WvletSeq}
import wvlet.core.tablet.{Column, Tablet, TabletWriter}
import xerial.lens.{ObjectSchema, Primitive, TextType, TypeConverter}

import scala.reflect.ClassTag

object ObjectWriter {

  def createTabletOf[A: ClassTag](name: String): Tablet = {
    val schema = ObjectSchema.of[A]
    val tabletColumnTypes: Seq[Column] = for (p <- schema.parameters) yield {
      val vt = p.valueType
      val columnType: Tablet.Type = vt match {
        case Primitive.Byte => Tablet.INTEGER
        case Primitive.Short => Tablet.INTEGER
        case Primitive.Int => Tablet.INTEGER
        case Primitive.Long => Tablet.INTEGER
        case Primitive.Float => Tablet.FLOAT
        case Primitive.Double => Tablet.FLOAT
        case Primitive.Char => Tablet.STRING
        case Primitive.Boolean => Tablet.BOOLEAN
        case TextType.String => Tablet.STRING
        case TextType.File => Tablet.STRING
        case TextType.Date => Tablet.STRING
        case _ =>
          // TODO support Option, Array, Map, the other types etc.
          Tablet.STRING
      }
      Column(p.name, columnType)
    }
    Tablet(name, tabletColumnTypes)
  }

}

/**
  *
  */
class ObjectWriter[A: ClassTag](name: String, output:TabletWriter) extends Wvlet[A, Tablet] {

  import ObjectWriter._

  val schema = ObjectSchema.of[A]

  // TODO Create data conversion operator using Tablet
  val tablet = createTabletOf[A](name)


  override def |[R](next: Wvlet[Tablet, R]): Wvlet[A, R] = ???
  override def apply(in: Seq[A]): WvletSeq[Tablet] = {



  }

  def write(record: A) {
    output.writeRecord {
      for(p <- schema.parameters) {
        val v = p.get(record)
        if (v == null) {
          output.writeNull
        }
        else {
          p.valueType match {
            case Primitive.Byte | Primitive.Short | Primitive.Int | Primitive.Long =>
              TypeConverter.convertToPrimitive(v, Primitive.Long) match {
                case Some(l) => output.writeLong(l)
                case None => output.writeNull
              }
            case Primitive.Float | Primitive.Double =>
              TypeConverter.convertToPrimitive(v, Primitive.Double) match {
                case Some(d) => output.writeDouble(d)
                case None => output.writeNull
              }
            case Primitive.Boolean =>
              TypeConverter.convertToPrimitive(v, Primitive.Boolean) match {
                case Some(b) => output.writeBoolean(b)
                case None => output.writeNull
              }
            case Primitive.Char | TextType.String | TextType.File | TextType.Date =>
              output.writeString(v.toString)
            case other =>
              // TODO support Array, Map, etc.
              output.writeString(other.toString())
          }
        }
      }
    }
  }
}
