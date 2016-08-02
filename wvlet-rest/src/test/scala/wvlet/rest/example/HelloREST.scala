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
package wvlet.rest.example

import wvlet.rest._
import com.twitter.finagle.http.{Request, Response}

/**
  *
  */
@Path("/")
class HelloREST {

  @GET
  def index = {
    "Hello REST!"
  }

  @GET
  def page(index:Int) = {
    "Page #{index}"
  }

  @GET
  def context(@Context request:Request, @Context response:Response) {
    //response.getContent.write
  }

  @GET
  def array_params(value:Seq[String]) = {
    s"[${value.mkString(",")}]"
  }

  @GET
  @Path("/user/$id")
  def user(id:String) = {
    s"user:${id}"
  }



}

