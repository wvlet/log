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

import java.util.concurrent.atomic.AtomicInteger

import wvlet.obj.tag.@@
import wvlet.test.WvletSpec

//--------------------------------------
//
// PrimitiveTest.scalaSince: 2012/07/17 22:30
//
//--------------------------------------
object ObjectTypeTest {
  case class Person(id:Int, name:String)

  trait Employee
  trait Customer
  trait Guest

}

/**
 * @author leo
 */
class ObjectTypeTest extends WvletSpec {

  "Primitive" should {
    "have all primitives" in {
      val names = (for (each <- Primitive.values) yield {
        each.name
      }).toList

      for(p <- Seq("Boolean", "Short", "Byte", "Char", "Int", "Float", "Long", "Double")) {
        names should (contain (p))
      }
    }

    "have name" in {
      Primitive.Int.name should be("Int")
      Primitive.Float.name should be("Float")
    }
  }

  "ObjectType" should {
    "detect types using Scala 2.10 reflection" in {
      val t = ObjectType(Seq(1, 3, 5))
      t match {
        case SeqType(_, Primitive.Int) => // OK
        case _ => fail(f"unexpected type: $t")
      }

      val t2 = ObjectType(Seq(Seq(1,2)))
      t2 match {
        case SeqType(_, SeqType(_, Primitive.Int)) => // OK
        case _ => fail(f"unexpected type: $t2")
      }
    }
    import ObjectTypeTest._

    "inspect case classes" in {
      val p = Person(1, "leo")
      val t = ObjectType(p)
      t.name should be ("Person")
    }

    "inspect case classes in Seq" in {
      val t = ObjectType(Seq[Any](Person(1, "leo")))
    }

    "detect generic types" taggedAs("gen") in {
      val st = ObjectType(Seq("hello"))
      debug(f"raw type ${st.rawType}")
      st.name should be ("Seq[String]")

      val mt = ObjectType(collection.mutable.Map(1 -> "leo"))
      debug(f"raw type ${mt.rawType}")
      mt.name should be ("Map[Int, String]")

      val im = ObjectType(Map(2 -> 1.4))
      debug(f"raw type ${im.rawType}")
    }

    "detect constructor params" taggedAs("cc") in {
      val t = new StandardType[Person](classOf[Person])
      debug(t.constructorParams)
    }

    "treat vector type as Seq" taggedAs("vector") in {
      val v = Vector(1, 2, 3)
      val t = ObjectType(v)
      debug(t)

      t match {
        case SeqType(_, Primitive.Int) => // OK
        case _ => fail
      }
    }

    "be comparable" in {
      val t1 = ObjectType.ofTypeTag[Person @@ Employee]
      val t2 = ObjectType.ofTypeTag[Person @@ Customer]
      val t3 = ObjectType.ofTypeTag[Person @@ Guest]

      val set = Set(t1, t2)
      set should contain (ObjectType.ofTypeTag[Person @@ Employee])
      set should contain (ObjectType.ofTypeTag[Person @@ Customer])
      set should not contain (ObjectType.ofTypeTag[Person @@ Guest])

      set should contain (t1)
      set should contain (t2)
      set should not contain (t3)

      val c = ObjectType.ofTypeTag[AtomicInteger @@ Employee]
      val s = Set(c)
      s should contain (ObjectType.ofTypeTag[AtomicInteger @@ Employee])
      s should contain (c)
    }
  }

}