package wvlet.rest

import javax.servlet._
import xerial.core.log.Logger
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import xerial.core.io.Resource
import xerial.lens._
import java.lang.reflect.{Modifier, Method}
import scala.language.existentials

object RequestDispatcher {

  /**
    * Base trait for matching each path component
    */
  sealed trait PathPattern {
    def isValid(value:String) : Boolean
  }

  /**
    * Specifies a binding of a path component to a variable
    * @param name
    * @param param
    */
  case class VarBind(name:String, param:MethodParameter) extends PathPattern {
    def isValid(value: String) = {
      TypeConverter.convert(value, param.valueType) match {
        case Some(x) => true
        case None => false
      }
    }
  }

  /**
    * Specifies an exact match for a path component
    * @param name
    */
  case class PathMatch(name:String) extends PathPattern {
    def isValid(value: String) = name == value.toLowerCase
  }

  /**
    * Mapping from path name to holder
    * @param name
    * @param appCls
    * @param methodMappings
    */
  case class WebActionMapping(name:String, appCls:Class[_], methodMappings:Seq[MethodMapping]) {
    override def toString = s"/$name => ${appCls.getSimpleName}:\n  ${methodMappings.mkString("\n  ")}"
    def findMapping(pathComponents:Seq[String]) = {
      val m = methodMappings.find(m => m.isValid(pathComponents))
      m.map(_.createMapping(pathComponents)).map((m.get, _))
    }
  }

  /**
    * Map path patterns to methods in a WebAction
    * @param pattern
    * @param actionMethod
    */
  case class MethodMapping(pattern:Seq[PathPattern], actionMethod:ObjectMethod) {
    override def toString = s"${pattern.mkString(", ")} -> $actionMethod"
    def name = actionMethod.name
    def isValid(pathComponents:Seq[String]) : Boolean = {
      if(pathComponents.size != pattern.size)
        false
      else
        pathComponents.zip(pattern).forall{ case (pc:String, pat:PathPattern) => pat.isValid(pc) }
    }
    def createMapping(pathComponents:Seq[String]) = {
      val result = pathComponents.zip(pattern).collect{
        case (pc, VarBind(name, param)) => param -> TypeConverter.convert(pc, param.valueType).get
      }
      result
    }
  }
}


/**
  * @author Taro L. Saito
  */
class RequestDispatcher extends Filter with Logger {

  import RequestDispatcher._

  val mappingTable = collection.mutable.Map[String, WebActionMapping]()

  def init(filterConfig: FilterConfig) {
    debug(s"Initialize the request dispatcher")
    // Initialize the URL mapping

    def isPublic(m:Method) = {
      Modifier.isPublic(m.getModifiers)
    }
    def isVoid(m:Method) = {
      m.getReturnType == Void.TYPE
    }
    def findClass(name:String) : Option[Class[_]] = {
      try
        Some(Class.forName(name))
      catch {
        case e:Exception => None
      }
    }
    def componentName(path: String): Option[String] = {
      val dot: Int = path.lastIndexOf(".")
      if (dot <= 0)
        None
      else
        Some(path.substring(0, dot).replaceAll("/", "."))
    }

    // Search webui.app package for WebAction classes
    val packagePath = "xerial.silk.webui.app"
    val rl = Resource.listResources(packagePath).filter(p => p.logicalPath.endsWith(".class") && !p.logicalPath.contains("$anon"))


    // Find public methods that return nothing (void return type)
    val mappings = for{
      resource <- rl.par
      componentName <- componentName(resource.logicalPath)
      cls <- findClass(s"${packagePath}.${componentName}") // TODO: if classOf[WebAction].isAssignableFrom(cls)
    } yield {
      val appName = {
        val name = cls.getSimpleName.toLowerCase
        if(name == "root")
          "/"
        else
          name
      }
      trace(s"app class: $cls")
      val methodMappers = for(method <- ObjectSchema.methodsOf(cls) if isPublic(method.jMethod) && isVoid(method.jMethod) && method.name != "init") yield {
        trace(s"found an action method: ${method}")
        val pathAnnotation = method.findAnnotationOf[Path]
        if(pathAnnotation.isDefined)  {
          val patterns = for(pc <- splitComponent(pathAnnotation.get.value)) yield {
            if(pc.startsWith("$")) {
              val valName = pc.stripPrefix("$")
              val valType = method.params.find(p => p.name == valName)
              if(valType.isEmpty)
                throw new IllegalArgumentException(s"no param ${valName} is found in method $method")
              VarBind(pc.stripPrefix("$"), valType.get)
            }
            else
              PathMatch(pc)
          }
          MethodMapping(patterns, method)
        }
        else
          MethodMapping(Seq(PathMatch(method.name.toLowerCase)), method)
      }
      appName -> WebActionMapping(appName, cls, methodMappers)
    }

    mappingTable ++= mappings.seq

    trace(s"Mapping table:\n${mappingTable.mkString("\n")}")
    debug(s"done.")
  }


  private def splitComponent(p:String) = p.stripPrefix("/").split("/")

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val req = request.asInstanceOf[HttpServletRequest]
    val res = response.asInstanceOf[HttpServletResponse]
    trace(s"doFilter: ${req.getRequestURI}")

    val path = req.getRequestURI
    val pc = splitComponent(path)

    /**
      * Set servlet parameters to the WebAction class
      * @param appCls
      * @return
      */
    def prepareApp(appCls:Class[_]) = {
      //trace(s"app class: $appCls")
      val app = appCls.newInstance()
      val m = appCls.getDeclaredMethod("init", classOf[HttpServletRequest], classOf[HttpServletResponse])
      m.invoke(app, req, res)
      app.asInstanceOf[AnyRef]
    }

    // Path examples:
    //  1. /<action name>/<method name>?p1=v1&...
    //  2. /<action name>/(<val bind>|<path name>/)+?p1=v1&p2=v2
    //  3. /<method name>?p1=v1&...


    if(pc.length > 0) {
      val appName = if(pc.length >=2) pc(0).toLowerCase else "/"
      val tail = if(pc.length >=2) pc.drop(1) else pc
      //debug(s"tail: ${tail.mkString(", ")}, mappingTable:${mappingTable(appName)}")
      for{
        am @ WebActionMapping(name, appCls, matchers) <- mappingTable.get(appName)
        (action, mapping) <- am.findMapping(tail)
      }
      {
        //trace(s"found mapping: $name/${action.name}, $mapping")
        val app = prepareApp(appCls)
        val mb = new MethodCallBuilder(action.actionMethod, app)
        for((param:MethodParameter, value) <- mapping) {
          mb.set(param.name, value)
        }

        // Set query paramters
        val query = req.getQueryString
        if(query != null) {
          for(q <- query.split("&")) {
            val c = q.split("=")
            if(c.length == 2)
              mb.set(c(0), c(1))
            else
              warn(s"invalid query string: $q")
          }
        }
        mb.execute
        return
      }
    }

    chain.doFilter(req, response)
  }


  def destroy() {

  }
}