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

