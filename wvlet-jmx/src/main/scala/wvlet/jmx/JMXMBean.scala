package wvlet.jmx

import java.lang.annotation.Annotation
import java.lang.reflect.Method
import javax.management._

import wvlet.obj.{ObjectMethod, ObjectSchema, Parameter}

/**
  * Expose object information using DynamicMBean
  */
case class JMXMBean(obj: AnyRef, mBeanInfo: MBeanInfo, attributes: Seq[ObjectMethod]) extends DynamicMBean {
  assert(obj != null)
  private lazy val attributeTable = attributes.map(a => a.name -> a).toMap

  override def getAttributes(attributes: Array[String]): AttributeList = {
    val l = new AttributeList(attributes.length)
    for (a <- attributes) {
      l.add(getAttribute(a))
    }
    l
  }
  override def getAttribute(attribute: String): AnyRef = {
    attributeTable.get(attribute) match {
      case Some(a) => a.invoke(obj).asInstanceOf[AnyRef]
      case None =>
        throw new AttributeNotFoundException(s"${attribute} is not found in ${obj.getClass.getName}")
    }
  }
  override def getMBeanInfo: MBeanInfo = mBeanInfo

  override def setAttributes(attributes: AttributeList): AttributeList = {
    val l = new AttributeList(attributes.size())
    import scala.collection.JavaConversions._
    for (a <- attributes.asList().toSeq) {
      l.add(setAttribute(a))
    }
    l
  }
  override def setAttribute(attribute: Attribute): Unit = {
    throw new AttributeNotFoundException(s"Setter for ${attribute.getName} is not found in ${obj.getClass.getName}")
//    attributeTable.get(attribute.getName) match {
//      case Some(a) => a.set(obj, attribute.getValue)
//      case None =>
//
//    }
  }
  override def invoke(actionName: String, params: Array[AnyRef], signature: Array[String]): AnyRef = {
    throw new UnsupportedOperationException(s"JMXMBean.invoke is not supported")
  }
}

object JMXMBean {

  private case class JMXMethod(m: ObjectMethod, jmxAnnotation: JMX)

  def of[A](obj: A): JMXMBean = {
    val cl = obj.getClass
    val schema = ObjectSchema(cl)
    val jmxMethods =
      schema
      .methods
      .collect {
        case m if m.findAnnotationOf[JMX].isDefined =>
          JMXMethod(m, m.findAnnotationOf[JMX].get)
      }

    val description = cl.getAnnotation(classOf[JMX]) match {
      case a if a != null => a.description()
      case _ => ""
    }

    val attrInfo = jmxMethods.map { x =>
      val desc = new ImmutableDescriptor()
      new MBeanAttributeInfo(
        x.m.name,
        x.m.returnType.rawType.getName,
        x.jmxAnnotation.description(),
        true,
        false,
        false
      )
    }

    val mbeanInfo = new MBeanInfo(
      cl.getName,
      description,
      attrInfo.toArray[MBeanAttributeInfo],
      Array.empty[MBeanConstructorInfo],
      Array.empty[MBeanOperationInfo],
      Array.empty[MBeanNotificationInfo]
    )

    new JMXMBean(obj.asInstanceOf[AnyRef], mbeanInfo, jmxMethods.map(_.m))
  }

  def collectUniqueAnnotations(m: Method): Seq[Annotation] = {
    collectUniqueAnnotations(m.getAnnotations)
  }

  def collectUniqueAnnotations(lst: Array[Annotation]): Seq[Annotation] = {
    var seen = Set.empty[Annotation]
    val result = Seq.newBuilder[Annotation]

    def loop(lst: Array[Annotation]) {
      for (a <- lst) {
        if (!seen.contains(a)) {
          seen += a
          result += a
          loop(a.annotationType().getAnnotations)
        }
      }
    }
    loop(lst)
    result.result()
  }

//  def buildDescription(a:Annotation) {
//    for(m <- a.annotationType().getMethods.toSeq) {
//      m.getAnnotation(classOf[DescriptorKey]) match {
//        case descriptorKey if descriptorKey != null =>
//          val name = descriptorKey.value()
//          Try(m.invoke(a)).map { v =>
//
//          }
//
//      }
//
//    }
//  }
//

}

