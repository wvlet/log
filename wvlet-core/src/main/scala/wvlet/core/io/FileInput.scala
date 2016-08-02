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
