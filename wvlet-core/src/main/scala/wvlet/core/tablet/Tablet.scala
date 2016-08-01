package wvlet.core.tablet

object Tablet {
  sealed trait Type {
    //def isPrimitive : Boolean
    //def javaType: Class[_]
  }

  case object INTEGER extends Type
  case object FLOAT extends Type
  case object BOOLEAN extends Type
  case object STRING extends Type
  case object TIMESTAMP extends Type
  case object JSON extends Type
  case object BINARY extends Type
  case class ARRAY(elemType: Type) extends Type
  case class MAP(keyType: Type, valueType: Type) extends Type
  case class RECORD(column: Seq[Column]) extends Type
}

case class Column(name: String, dataType: Tablet.Type)

case class Schema(name: String, column: Seq[Column]) {
  private lazy val columnIdx: Map[Column, Int] = column.zipWithIndex.toMap[Column, Int]

  def size: Int = column.size

  /**
    * @param index 0-origin index
    * @return
    */
  def columnType(index: Int) = column(index)

  /**
    * Return the column index
    *
    * @param column
    * @return
    */
  def columnIndex(column: Column) = columnIdx(column)
}

// TODO optimize the data structure
case class Record(buffer: Array[Byte])
case class Tablet(record: Seq[Record])

