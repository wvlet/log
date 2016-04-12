package wvlet.core

/**
  *
  */
trait RelationStreamWriter extends AutoCloseable {

  def pushContext[Context](c:Context)
  def popContext : Unit

  def withContext[Context, U](c:Context)(body:RelationStreamWriter => Unit) = {
    try {
      pushContext(c)
      body(this)
    }
    finally {
      popContext
    }
  }

  def addRelation[Relation](r:Relation) : Unit

}

trait RelationStreamReader extends AutoCloseable {

  def startContext[Context](c:Context)

  def endContext[Context](c:Context)

  def relation[Relation](r:Relation)

}

