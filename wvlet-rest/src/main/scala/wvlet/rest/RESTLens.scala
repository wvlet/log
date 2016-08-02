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
package wvlet.rest

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}


object RESTLens {

  class Builder(serviceClassList:Seq[Class[_]]) {

    def withService[A](serviceClass:Class[A]) : Builder = {
      new Builder(serviceClassList :+ serviceClass)
    }

//    def newService : Service = {
//
//      for(service <- serviceClassList) yield {
//
//
//
//      }
//
//
//      // TODO
//      null
//    }
  }


}


/**
  *
  */
class RESTLens(packageName:String) {


  def source(request:Request): Unit = {

  }


}
