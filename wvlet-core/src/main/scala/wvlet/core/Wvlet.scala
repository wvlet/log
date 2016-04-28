package wvlet.core


object Wvlet {

  sealed trait Type
  case object LONG extends Type
  case object DOUBLE extends Type
  case object BOOLEAN extends Type
  case object STRING extends Type
  case object TIMESTAMP extends Type
  case object JSON extends Type
  case object BINARY extends Type
  case class ARRAY(elemType: Type)
  case class MAP(keyType: Type, valueType: Type)

  case class Column(name: String, dataType: Type)

  case class Table(name: String, column: Seq[Column])
  trait Context {
    def name:String
  }
  case class Wvlet(name: String, tablets: Map[Context, Table])

  trait WvletInput {
    def wvlet: Wvlet

    

  }


}