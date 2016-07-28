package wvlet.core

import java.lang.reflect.Field

import sun.misc.Unsafe

/**
  *
  */
object Unsafe {
  private[wvlet] val unsafe: Unsafe = {
    // Check java version
    val javaVersion: String = System.getProperty("java.specification.version", "")
    val dotPos: Int = javaVersion.indexOf('.')
    var isJavaAtLeast7: Boolean = false
    if (dotPos != -1) {
      try {
        val major: Int = javaVersion.substring(0, dotPos).toInt
        val minor: Int = javaVersion.substring(dotPos + 1).toInt
        isJavaAtLeast7 = major > 1 || (major == 1 && minor >= 7)
      }
      catch {
        case e: NumberFormatException => {
          e.printStackTrace(System.err)
        }
      }
    }
    val field: Field = classOf[Unsafe].getDeclaredField("theUnsafe")
    field.setAccessible(true)
    val unsafeInstance = field.get(null).asInstanceOf[Unsafe]
    if (unsafeInstance == null) {
      throw new RuntimeException("Unsafe is unavailable")
    }
    unsafeInstance
  }

  require(unsafe != null, "sun.misc.Unsafe is not available")

}
