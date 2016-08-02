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
package wvlet.config

import wvlet.inject.Inject
import wvlet.log.LogSupport
import wvlet.obj.ObjectType

import scala.reflect.runtime.{universe => ru}

class ConfigImpl(holder: Seq[ConfigHolder]) extends Config with LogSupport {

  private def find[A](tpe: ObjectType): Option[Any] = {
    holder.find(x => x.tpe == tpe).map {_.value}
  }

  def of[ConfigType](implicit tag: ru.TypeTag[ConfigType]): ConfigType = {
    val t = ObjectType.ofTypeTag(tag)
    find(t) match {
      case Some(x) =>
        x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"No [${t}] value is found")
    }
  }

  override def bindConfigs(i: Inject): Unit = {
    for (c <- holder) {
      i.bind(c.tpe).toInstance(c.value)
    }
  }

  override def iterator: Iterator[ConfigHolder] = holder.iterator
}

