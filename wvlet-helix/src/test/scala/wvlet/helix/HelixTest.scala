package wvlet.helix

import java.io.{PrintStream, PrintWriter}
import javax.inject.Inject

import wvlet.log.LogSupport
import wvlet.test.WvletSpec

import scala.util.Random

case class ExecutorConfig(numThreads: Int)

object ServiceMixinExample {

  trait Printer {
    def print(s: String): Unit
  }

  case class ConsoleConfig(out:PrintStream)

  class ConsolePrinter(config:ConsoleConfig) extends Printer with LogSupport {
    info(s"using config: ${config}")

    def print(s: String) { config.out.println(s) }
  }

  class LogPrinter extends Printer with LogSupport {
    def print(s: String) {info(s)}
  }

  class Fortune {
    def generate: String = {
      val pattern = Seq("Hello", "How are you?")
      pattern(Random.nextInt(pattern.length))
    }
  }

  trait PrinterService {
    protected def printer = inject[Printer]
  }

  trait FortuneService {
    protected def fortune = inject[Fortune]
  }

  /**
    * Mix-in printer/fortune instances
    * Pros:
    * -
    * Cons:
    *   - Need to define XXXService boilerplate, which just has a val or def of the service object
    *   - Cannot change the variable name without defining additional XXXService trait
    *     - Need to care about variable naming conflict
    *
    */
  trait FortunePrinterMixin extends PrinterService with FortuneService {
    printer.print(fortune.generate)
  }

  /**
    * Using local val/def injection
    *
    * Pros:
    *   - Service reference (e.g., printer, fortune) can be scoped inside the trait.
    *   - You can save boilerplate code
    * Cons:
    *   - If you use the same service multiple location, you need to repeat the same description in the user module
    *   - To reuse it in other traits, we still need to care about the naming conflict
    */
  trait FortunePrinterEmbedded {
    protected def printer = inject[Printer]
    protected def fortune = inject[Fortune]

    printer.print(fortune.generate)
  }

  /**
    * Using Constructor for dependency injection (e.g., Guice)
    *
    * Pros:
    *   - Close to the traditional OO programming style
    *   - Can be used without DI framework
    * Cons:
    *   - To add/remove modules, we need to create another constructor or class.
    * -> code duplication occurs
    *   - Enhancing the class functionality
    *   - RememenbOrder of constructor arguments
    * -
    */
  class FortunePrinterAsClass @Inject() (printer: Printer, fortune: Fortune) {
    printer.print(fortune.generate)
  }

}

/**
  *
  */
class HelixTest extends WvletSpec {

  import wvlet.helix.ServiceMixinExample._

  "Helix" should {

    "instantiate class" in {

      val h = new Helix
      h.bind[Printer].to[ConsolePrinter]
      h.bind[ConsoleConfig].toInstance(ConsoleConfig(System.err))

      val context = h.getContext
      val m = context.weave[FortunePrinterMixin]
    }

  }
}
