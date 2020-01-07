package skutek.operations
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try


class ConcurrencyTest extends Specification {

  def is = simple ^ tricky

  case object FxC extends Concurrency
  def sleep_!(n: Int) = FxC.Run { Thread.sleep(n) }

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
        FxC.handler.await(900.millis).run(eff) 
      } 
    } must beSuccessfulTry
  }


  def tricky = br ^ "Locally handled State should not disrupt Concurrency" ! { 
    case object FxR extends Reader[String]
    case object FxW extends Writer[Vector[String]]
    case object FxS extends State[Int]

    val eff1 = for {
      n <- FxS.Get
      _ <- sleep_!(500)
      _ <- FxS.Put(n + 1)
      _ <- FxW.Tell(n.toString)
    } yield n

    val eff2 = FxS.handler(1).dropState.handle[FxW.type with FxC.type](eff1)

    val eff3 = for {
      s <- FxR.Ask
      n <- eff2
      _ <- sleep_!(500) *! sleep_!(500) *! sleep_!(500)
      _ <- FxW.Tell(s + n)
    } yield ()

    Try { 
      withThreadPool(5) { implicit ec =>
        val h = FxC.handler.await(1400.millis) <<<! FxR.handler("#") <<<! FxW.handler
        h run eff3
      } 
    } must beSuccessfulTry
  }
}
