wvlet-obj
======

A library for inspecting Object schema (e.g., parameter names and its types, constructor, etc.)

## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-obj_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-obj_2.11/)

```scala
libraryDependencies += "org.wvlet" %% "wvlet-obj" % "(version)"
```


```scala
import wvlet.obj.ObjectSchema

case class Person(id:Int, name:String, )

val scheam = ObjectSchema.of[Person]
```

ObjectSchema has detailed information of your class, including:
* constructor and its argument type and names!
* case class parameters
* methods

Extracting parameter names is impossible in Java programs, since JVM drops parameters names used in the source code at compile-time.  
While Scaal retains parameter names within the byte code of the class as ScalaSig. wvlet-obj reads ScalaSig and extract method parameter names and 
detailed types (e.g., type parameters used in Generics: e.g., `String` in `List[String]`). 

