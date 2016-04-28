package wvlet.core



trait RecordStore {

  def withContext[C, A](context:C, next:Class[A]) : RecordWriter[A]



}



trait RecordWriter[A] {
  def write(record:A)
}

trait RecordReader[A] {
  def hasNext : Boolean
  def read : A
}


