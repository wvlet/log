package wvlet

/**
  *
  */
package object core {

  def withResource[Resource <: AutoCloseable, U](resource:Resource)(body: Resource => U) : U = {
    try {
      body(resource)
    }
    finally {
      resource.close
    }
  }

  val objectLens = ""

}
