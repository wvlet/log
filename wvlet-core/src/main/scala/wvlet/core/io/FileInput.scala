package wvlet.core.io

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.nio.file.Path

import wvlet.core.io.IOUtil._

/**
  *
  */
class FileInput(file: Path) {
  def this(file: File) = this(file.toPath)
  def this(path: String) = this(new File(path))

  def open[U](body: InputStream => U): U = {
    withResource(new BufferedInputStream(new FileInputStream(file.toFile))) {
      body(_)
    }
  }
}
