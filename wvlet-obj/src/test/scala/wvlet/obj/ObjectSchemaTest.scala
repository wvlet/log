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

import org.scalatest.Tag
import wvlet.log.{LogLevel, Logger}
import wvlet.test.WvletSpec

import scala.io.Source

//--------------------------------------
//
// ObjectSchemaTest.scala
// Since: 2012/01/17 10:17
//
//--------------------------------------

object ObjectSchemaTest {

  class A(val id: Int, val name: String)

  class C(val flag: Option[Int])

}

class B(val id: Int, val flag: Option[Boolean])

/**
  * @author leo
  */
class ObjectSchemaTest extends WvletSpec {

  import ObjectSchemaTest._

  "ObjectSchema" should {
    "enumerate all fields" taggedAs ("schema") in {
      val s = ObjectSchema(classOf[A])
      debug(s)
      val c = s.constructor
      c.params.length shouldBe 2

      s.name shouldBe classOf[A].getSimpleName
      s.fullName shouldBe classOf[A].getName

      val attr = s.parameters
      attr.length shouldBe 2
      attr(0).name shouldBe "id"

      attr(0).rawType shouldBe classOf[Int]
      attr(1).name shouldBe "name"
      attr(1).rawType shouldBe classOf[String]
    }


    trait ClassFixture {
      val cg     = classOf[GlobalCls]
      val co     = classOf[ScalaClassLensTest.ClsInObj]
      val params = Array(
        ("id", classOf[Int]),
        ("flag", classOf[Option[Int]]),
        ("list", classOf[Array[String]]),
        ("map", classOf[Map[String, Float]]))
    }


    "find class definition containing ScalaSignature" in {
      new ClassFixture {
        val s1 = ObjectSchema(cg).findSignature
        val s2 = ObjectSchema(co).findSignature

        s1 should be('defined)
        s2 should be('defined)
      }
    }

    "find constructor parameters defined in global classes" in {
      new ClassFixture {
        val s  = ObjectSchema(cg)
        val cc = s.constructor

        //debug { p.mkString(", ") }
        val p = cc.params
        p.size shouldBe 4

        for (((name, t), i) <- params.zipWithIndex) {
          p(i).name shouldBe name
          When("type is " + t)
          p(i).rawType.isAssignableFrom(t)
        }
      }
    }

    "find constructor parameters defined in object classes" in {
      new ClassFixture {
        val cc = ObjectSchema(co).constructor
        //debug { p.mkString(", ") }
        val p  = cc.params
        p.size shouldBe 4

        for (((name, t), i) <- params.zipWithIndex) {
          p(i).name shouldBe name
          When("type is " + t)
          p(i).rawType.isAssignableFrom(t)
        }
      }
    }

    "find root constructor" in {
      val c1 = ObjectSchema.of[ScalaClassLensTest.ClsInObj].constructor
      debug {
        c1
      }
      val c2 = ObjectSchema.of[GlobalCls].constructor
      debug {
        c2
      }
    }


    "find attributes defined in class body" in {
      val c = ObjectSchema.of[ValInBody].parameters
      debug {
        "ValInBody: " + c.mkString(", ")
      }
      c.size shouldBe 3
    }

    "find methods" in {
      val c = ObjectSchema.of[MethodHolder].methods
      debug {
        c.mkString(", ")
      }

      c.size shouldBe 3
    }

    "find imported methods" in {
      val c = ObjectSchema.of[ImportSample].methods
      c.size should be(1)
    }



    "find parameters defined in extended traits" in {
      val schema = ObjectSchema.of[MixinSample]
      schema.parameters.length shouldBe 3
      schema.findParameter("param1") should be('defined)
      schema.findParameter("param2") should be('defined)
      schema.findParameter("paramA") should be('defined)

    }

    "find method with Array type arguments" taggedAs ("array") in {

      debug("Array[String] type name:" + classOf[Array[String]].getName)

      val s = ObjectSchema.of[ArrayMethodSample]
      val m = s.methods
      m.length shouldBe 1
      m(0).name shouldBe "main"
      m(0).params.length shouldBe 1
      m(0).params(0).name shouldBe "args"
    }

    "be safe when Array[A] is passed" in {
      val s = ObjectSchema.of[Array[String]]
      debug(s"schema:$s")
      s.name shouldBe "String[]"
    }

    "be safe when Seq[A] is passed" taggedAs ("opt-seq") in {
      val s = ObjectSchema.of[Seq[String]]
      debug(s"schema:$s")
      s.name shouldBe "Seq"
      //s.parameters.isEmpty must be (true)
      debug {
        val sigLines = Source.fromString(s.findSignature.map(_.toString).get).getLines()
        val hashVar = sigLines.filter(line => line.contains("hash"))
        hashVar.mkString("\n")
      }
    }

    "be safe when resolving the field owner of a private field defined in a Trait" in {
      val p = ObjectSchema.of[SampleA].parameters
      val obj = new SampleA
      p.collect {
        case f: FieldParameter => f.get(obj)
      }
    }

    "resolve method defined in companion object referenced from trait" taggedAs (Tag("trait-ref")) in {
      val m = ObjectSchema.of[SampleB].methods
      m should have size (2)
      m(0).name should be("hello")
      m(1).name should be("helloWithArg")

      val b = new SampleB {}
      val r = m(0).invoke(b)

      r should be("hello")
      m(1).invoke(b, "world") should be("hello world")
    }

    "resolve type alias" taggedAs ("alias") in {
      val schema = ObjectSchema.of[TypeAlias.Person]
      val p = schema.parameters(0)
      p.name shouldBe "pid"
      p.valueType shouldBe ObjectType.of(classOf[Int])

      val intType = ObjectType.ofTypeTag[Int]
      val m = schema.methods.find(_.name == "getPid")
      m shouldBe 'defined
      m.get.valueType shouldBe intType

      val l = schema.methods.find(_.name == "hasSamePid")
      l shouldBe 'defined
      l.get.params(0).valueType shouldBe intType
    }
  }

}

class GlobalCls(val id: Int, val flag: Option[Int], val list: Array[String], val map: Map[String, Float])

class ValInBody(val id: Int = 1) {
  val args: Seq[String] = Seq.empty
  val opt : Option[Int] = None
}

class MethodHolder {
  def hello: String = "hello"

  def hello(name: String): String = "hello " + name

  def methodWithOption(displayHelp: Option[Boolean]): Unit = {}
}

object ScalaClassLensTest {

  class ClsInObj(val id: Int, val flag: Option[Int], val list: Array[String], val map: Map[String, Float])

  class Dummy(val name: String) {
    def dummyMethod: Unit = {}
  }

}

trait SampleTrait1 {
  var param1: Boolean = false
}

trait SampleTrait2 {
  var param2: Int = 10
}

class MixinSample(val paramA: String) extends SampleTrait1 with SampleTrait2

class ArrayMethodSample {
  def main(args: Array[String]) {
    println("hello")
  }
}

object ImportSample {
  def helloWorld = "Hello World"
}

class ImportSample {
}

trait TraitWithPrivateField {
  private val a: String = "hello"
}

class SampleA extends TraitWithPrivateField {

}

trait SampleB
object SampleB {
  def hello = "hello"
  def helloWithArg(m: String): String = "hello " + m
}

object TypeAlias {
  type PersonID = Int
  case class Person(pid: PersonID) {
    def getPid: PersonID = pid

    def hasSamePid(other:PersonID) : Boolean = {
      pid == other
    }
  }
}