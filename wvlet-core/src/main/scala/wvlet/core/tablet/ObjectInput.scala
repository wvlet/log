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

import org.msgpack.core.MessagePack
import wvlet.core._
import wvlet.log.LogSupport
import wvlet.obj.{ObjectSchema, Primitive, TextType, TypeUtil}

import scala.reflect.runtime.{universe => ru}

object ObjectWriter {

  def createScheamOf[A: ru.TypeTag](name: String): Schema = {
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
    Schema(name, tabletColumnTypes)
  }

}

/**
  *
  */
class ObjectInput() extends Input with LogSupport {

  // TODO Create data conversion operator using Tablet
  //val tablet = createSchemaOf[A](name)

  def write(record: Any): Record = {
    // TODO optimize buffer allocation
    val packer = MessagePack.newDefaultBufferPacker()

    if (record == null) {
      packer.packNil()
    }
    else {
      val objSchema = ObjectSchema(record.getClass)
      //val arrSize = Math.max(objSchema.parameters.length, schema.size)
      // TODO add parameter values not in the schema
      info(objSchema)

      packer.packArrayHeader(objSchema.parameters.length)
      for (p <- objSchema.parameters) {
        val v = p.get(record)
        if (v == null) {
          packer.packNil()
        }
        else {
          p.valueType match {
            case Primitive.Byte | Primitive.Short | Primitive.Int | Primitive.Long =>
              packer.packLong(v.toString.toLong)
            case Primitive.Float | Primitive.Double =>
              packer.packDouble(v.toString.toDouble)
            case Primitive.Boolean =>
              packer.packBoolean(v.toString.toBoolean)
            case Primitive.Char | TextType.String | TextType.File | TextType.Date =>
              packer.packString(v.toString)
            case arr if TypeUtil.isArray(v.getClass) =>
              // TODO FIXME
              val seq = arr.asInstanceOf[Seq[_]]
              packer.packArrayHeader(seq.length)
              for (s <- seq) {
                packer.packString("test")
              }
            case other =>
              // TODO support Array, Map, etc.
              packer.packString(other.toString())
          }
        }
      }
    }
    Record(packer.toByteArray)
  }
}


