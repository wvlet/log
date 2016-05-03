package wvlet.core.tablet
import wvlet.core.tablet.TabletReader.Line
import wvlet.core.time.TimeStamp


trait TextTabletReader extends TabletReader {

  def readLine[U](line:Line)(body: TabletReader => U)

}


/**
  *
  */
object TextTabletReader {

  class TSVTabletReader(schema:Schema) extends TextTabletReader {

    def readLine[U](line: Line)(body: TabletReader => U) {
      val cols = line.split("\t")



    }



    override def readLong: Long = ???
    // TODO type resolution of array and map elements
    override def readArray: Seq[_] = ???
    override def readNull: Unit = ???
    override def readTimestamp: TimeStamp = ???
    override def readMap: Map[_, _] = ???
    override def isNull: Boolean = ???
    override def readString: String = ???
    override def readDouble: Double = ???
    override def readBinary: Array[Byte] = ???
    override def readJson: String = ???

  }
}
