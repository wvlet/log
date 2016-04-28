package wvlet.core.io

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.nio.file.Path

import wvlet.core.Wvlet.Input
import wvlet.core._


/**
  *
  */
class FileInput(file:Path) extends Input {
  def this(file:File) = this(file.toPath)
  def this(path:String) = this(new File(path))

  def open[U](body: InputStream => U) : U = {
    withResource(new BufferedInputStream(new FileInputStream(file.toFile))) {
      body(_)
    }
  }
}
