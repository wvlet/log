package wvlet.helix

import wvlet.helix.HelixException.ErrorType
import wvlet.obj.ObjectType

object HelixException {

  sealed trait ErrorType {
    def errorCode: String = this.toString
  }
  case class CYCLIC_DEPENDENCY(deps: Set[ObjectType]) extends ErrorType

}
/**
  *
  */
class HelixException(errorType: ErrorType) extends Exception(errorType.toString) {

}


