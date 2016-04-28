package wvlet.`object`

import wvlet.core.tablet.{Column, Tablet, TabletWriter}
import xerial.lens.{ObjectSchema, Primitive, TextType}

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
class ObjectWriter[A: ClassTag](name: String, output:TabletWriter) {

  import ObjectWriter._

  val schema = ObjectSchema.of[A]
  val tablet = createTabletOf[A](name)



  def write(record: A) {
    output.writeRecord {
      


    }
  }

}
