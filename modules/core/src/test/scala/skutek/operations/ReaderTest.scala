package skutek.operations
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class ReaderTest extends Specification {

  def is = List(ask, asks, localmod).reduce(_ and _)

  def ask = {
    case object Fx extends Reader[Int]

    Fx.Ask.runWith(Fx.handler(42)) must_== 42
  }

  def asks = {
    type Env = (Int, String)
    case object Fx extends Reader[Env]

    (for {
      i <- Fx.Asks(_._1)
      s <- Fx.Asks(_._2)
    } yield (i, s))
    .runWith(Fx.handler((42, "foo"))) must_== (42, "foo")
  }

  def localmod = {
    case object FxR extends Reader[Int]
    case object FxW extends Writer[Vector[String]]

    def loop(str: String): Unit !! FxR.type with FxW.type = {
      if (str.isEmpty)
        Return
      else 
        str.head match {
          case '[' => FxR.LocalMod(_ + 1)(loop(str.tail))
          case ']' => FxR.LocalMod(_ - 1)(loop(str.tail))
          case x => for { 
            indent <- FxR.Ask
            _ <- FxW.Tell(("  " * indent) :+ x)
            _ <- loop(str.tail) 
          } yield ()
        }
    }

    val lines1 = 
      loop("ab[cd[e]f[]g]h")
      .runWith(FxR.handler(0) <<<! FxW.handler.justState)
      .mkString("\n")

    val lines2 = """
      |a
      |b
      |  c
      |  d
      |    e
      |  f
      |  g
      |h
      |""".stripMargin.tail.init

    lines1 must_== lines2
  }
}
