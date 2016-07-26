package wvlet.core.io

import java.io._
import java.net.ServerSocket
import java.nio.charset.StandardCharsets

/**
  *
  */
object IOUtil {

  def withResource[Resource <: AutoCloseable, U](resource: Resource)(body: Resource => U): U = {
    try {
      body(resource)
    }
    finally {
      resource.close
    }
  }

  def unusedPort: Int = {
    withResource(new ServerSocket(0)) { socket =>
      socket.getLocalPort
    }
  }

  def findPath(path: String): File = findPath(new File(path))

  def findPath(path: File): File = {
    if (path.exists()) {
      path
    }
    else {
      val defaultPath = new File(new File(System.getProperty("prog.home", "")), path.getPath)
      if (!defaultPath.exists()) {
        throw new FileNotFoundException(s"${path} is not found")
      }
      defaultPath
    }
  }

  def readAsString(resourcePath: String) = {
    require(resourcePath != null, s"resourcePath is null")
    val file = findPath(new File(resourcePath))
    if (!file.exists()) {
      throw new FileNotFoundException(s"${file} is not found")
    }
    readFully(new FileInputStream(file)) {
      data => new String(data, StandardCharsets.UTF_8)
    }
  }

  def readFully[U](in: InputStream)(f: Array[Byte] => U): U = {
    val byteArray = withResource(new ByteArrayOutputStream) {
      b =>
        val buf = new Array[Byte](8192)
        withResource(in) {
          src =>
            var readBytes = 0
            while ( {
              readBytes = src.read(buf);
              readBytes != -1
            }) {
              b.write(buf, 0, readBytes)
            }
        }
        b.toByteArray
    }
    f(byteArray)
  }

}
