// This example is based on the problem presented in thread:
// https://www.reddit.com/r/scala/comments/6pmoah/running_dependent_futures_in_parallel/
package skutek_examples
import scala.concurrent.duration._
import skutek._

object InvalidFutures {

  def beBusy[A, E](dur: Duration, failMe: Boolean, failure : => E, success : => A) = 
    RunEff {
      Thread.sleep(dur.toMillis)
      if (failMe)
        Invalid(failure)
      else
        Return(success)
    }


  def makeEff(failA: Boolean, failB: Boolean, failC: Boolean, failD: Boolean) = {
    def futureA                       = beBusy(10.millis   , failMe=failA , "a failed", "a")
    def futureB(a: String)            = beBusy(1000.millis , failMe=failB , "b failed", a + "b")
    def futureC(a: String)            = beBusy(1000.millis , failMe=failC , "c failed", a + "c")
    def futureD(b: String, c: String) = beBusy(10.millis   , failMe=failD , "d failed", b + c + "d")

    for {
      a <- futureA
      workaround <- futureB(a) *! futureC(a) 
      (b, c) = workaround
      d <- futureD(b, c)
    } yield d
  }


  def time[A](what: String)(stuff : => A): A = {
    val t0 = System.currentTimeMillis()
    val result = stuff
    val t = System.currentTimeMillis() - t0
    println("%s: %d.%03dms".format(what, t / 1000, t % 1000))
    result
  }


  def apply() = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val eff = time("Creating effect") { makeEff(false, true, true, false) }
  
    val handler = ConcurrencyHandler().await(1100.millis) +! ValidationHandler[String]

    time("Handling effect") { handler.run(eff) } match {
      case Right(x) => println(s"Success: $x")
      case Left(x) => println(s"Failure: $x")
    }
  }
}
