package wvlet.rest

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}


object RESTLens {

  class Builder(serviceClassList:Seq[Class[_]]) {

    def withService[A](serviceClass:Class[A]) : Builder = {
      new Builder(serviceClassList :+ serviceClass)
    }

    def newService : Service = {

      for(service <- serviceClassList) yield {



      }



    }
  }

}


/**
  *
  */
class RESTLens(packageName:String) {


  def source(request:Request): Unit = {

  }


}
