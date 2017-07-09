package skutek
import org.specs2._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try



class ConcurrencyTests extends Specification {

  def is = simple ^ tricky

  def sleep_!(n: Int) = Run { Thread.sleep(n) }

  def withThreadPool[A](n: Int)(f: ExecutionContext => A): A = {
    val tp = java.util.concurrent.Executors.newFixedThreadPool(n)
    val ec = ExecutionContext.fromExecutorService(tp)
    try {
      f(ec)
    }
    finally {
      tp.shutdownNow()
    }
  }

  def simple = br ^ "Concurrency operations should work" ! {
    val eff = sleep_!(500) *! sleep_!(500) *! sleep_!(500)
    Try { 
      withThreadPool(3) { implicit ec =>
        ConcurrencyHandler().await(700.millis).run(eff) 
      } 
    } must beSuccessfulTry
  }


  def tricky = br ^ "Locally handled State should not disrupt Concurrency" ! { 
    val eff1 = for {
      n <- Get[Int]
      _ <- sleep_!(500)
      _ <- Put(n + 1)
      _ <- Tell(n.toString)
    } yield n

    val eff2 = StateHandler(1).eval.fx[Concurrency].fx[Writer[String]].handle(eff1)

    val eff3 = for {
      s <- Ask[String]
      n <- eff2
      _ <- sleep_!(500) *! sleep_!(500) *! sleep_!(500)
      _ <- Tell(s + n)
    } yield ()

    Try { 
      withThreadPool(5) { implicit ec =>
        val h = ConcurrencyHandler().await(1200.millis) +! ReaderHandler("#") +! WriterHandler.strings
        h run eff3
      } 
    } must beSuccessfulTry
  }
}
