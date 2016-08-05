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

package wvlet.opts

import wvlet.log.LogSupport
import wvlet.obj.{MethodParameter, ObjectMethod, StandardBuilder, TypeUtil}

//--------------------------------------
//
// MethodCallBuilder.scala
// Since: 2012/03/27 16:43
//
//--------------------------------------

/**
  * Builds method call arguments
  *
  * @author leo
  */
class MethodCallBuilder(m: ObjectMethod, owner: AnyRef) extends StandardBuilder[MethodParameter] with LogSupport {

  // Find the default arguments of the method
  protected def defaultValues = (for (p <- m.params; v <- findDefaultValue(p.name)) yield p.name -> v).toMap

  protected def findParameter(name: String): Option[MethodParameter] = {
    val cname = CName(name)
    m.params.find(p => CName(p.name) == cname)
  }

  private def findDefaultValue(name: String): Option[Any] = {
    findParameter(name).flatMap { p =>
      try {
        val methodName = "%s$default$%d".format(m.name, p.index + 1)
        val dm = owner.getClass.getMethod(methodName)
        Some(dm.invoke(owner))
      }
      catch {
        case _: Throwable => None
      }
    }
  }

  def execute: Any = {
    trace(s"holder: $holder")
    val args = for (p <- m.params) yield {
      (get(p.name) getOrElse TypeUtil.zero(p.rawType, p.valueType)).asInstanceOf[AnyRef]
    }
    trace {"args: " + args.mkString(", ")}
    if (args.isEmpty) {
      m.jMethod.invoke(owner)
    }
    else {
      m.jMethod.invoke(owner, args: _*)
    }
  }

}

