package wvlet.core.tablet

import org.msgpack.core.MessagePacker
import wvlet.core.rx.Flow

trait TabletWriter {
  //def clearRecord
  //def getRecord : Seq[String]

  def writeRecord(schema: Schema, flow: Flow[Record])(body: MessagePacker => Unit)
}

