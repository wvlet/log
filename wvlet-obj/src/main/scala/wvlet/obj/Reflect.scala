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
package wvlet.obj

/**
  * Reflection utility functions
  */
object Reflect {

  import java.lang.{reflect => jr}

  /**
    * Set the accessibility flag of fields and methods if they are not accessible, then
    * do some operation, and reset the accessibility properly upon the completion.
    */
  private[obj] def access[A <: jr.AccessibleObject, B](f: A)(body: => B): B = {
    synchronized {
      val accessible = f.isAccessible
      try {
        if (!accessible)
          f.setAccessible(true)
        body
      }
      finally {
        if (!accessible)
          f.setAccessible(false)
      }
    }
  }

  def readField(obj: Any, f: jr.Field): Any = {
    access(f) {
      f.get(obj)
    }
  }

  /**
    * Update the field value in the given object.
    *
    * @param obj
    * @param f
    * @param value
    */
  def setField(obj: Any, f: jr.Field, value: Any): Unit = {
    import TypeUtil._

    def prepareInstance(prevValue: Option[_], newValue: Any, targetType: Class[_]): Option[_] = {
      if (isOption(targetType) && !isOption(newValue.getClass)) {
        val elementType = getTypeParameters(f)(0)
        Some(prepareInstance(prevValue, newValue, elementType))
      }
      else
        Some(TypeConverter.convert(newValue, targetType))
    }


    access(f) {
      val fieldType = f.getType
      val currentValue = f.get(obj)
      val newValue = prepareInstance(Some(currentValue), value, fieldType)
      if (newValue.isDefined)
        f.set(obj, newValue.get)
    }

  }

  /**
    * Get type parameters of the field
    * @param f
    * @return
    */
  def getTypeParameters(f: jr.Field): Array[Class[_]] = {
    getTypeParameters(f.getGenericType)
  }

  /**
    *
    * @param gt
    * @return
    */
  def getTypeParameters(gt: jr.Type): Array[Class[_]] = {
    gt match {
      case p: jr.ParameterizedType => {
        p.getActualTypeArguments.map(resolveClassType(_)).toArray
      }
    }
  }

  /**
    *
    * @param t
    * @return
    */
  def resolveClassType(t: jr.Type): Class[_] = {
    t match {
      case p: jr.ParameterizedType => p.getRawType.asInstanceOf[Class[_]]
      case c: Class[_] => c
      case _ => classOf[Any]
    }
  }


}