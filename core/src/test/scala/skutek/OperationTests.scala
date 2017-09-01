package skutek
import org.specs2._


class OperationTests extends Specification with CanLaunchTheMissiles {

  def is = List(reader, writer, state, choice, validation, maybe, except).reduce(_^_)

  def state = br ^ "State operations should work" ! {
    (for {
      a <- Get[Int]
      _ <- Put(a + 99)
      _ <- Put(a + 10)
      b <- Get[Int]
      _ <- Put(a + 999)
      _ <- Put(a + 100)
      c <- Get[Int]
      _ <- Put(a + 9999)
      _ <- Put(c + 1000)
    } yield ())
    .runWith(StateHandler(1).exec) must_== 1101
  }


  def choice = br ^ "Choice operations should work" ! {
    val eff = for {
      i <- Choose(0 to 3)
      if i % 2 != 0
      c <- Choose('a' to 'c')
    } yield s"$i$c"

    List(
      eff.runWith(ChoiceHandler) must_== Vector("1a", "1b", "1c", "3a", "3b", "3c"),
      eff.runWith(ChoiceHandler.FindFirst) must_== Some("1a")
    ).reduce(_ and _)
  }


  def reader = br ^ "Reader operations should work" ! {
    def loop(str: String): Unit !! Reader[Int] with Writer[String] = {
      if (str.isEmpty)
        Return()
      else 
        str.head match {
          case '[' => LocalMod[Int](_ + 1)(loop(str.tail))
          case ']' => LocalMod[Int](_ - 1)(loop(str.tail))
          case x => for { 
            indent <- Ask[Int]
            _ <- Tell(("  " * indent) :+ x)
            _ <- loop(str.tail) 
          } yield ()
        }
    }

    val lines1 = loop("ab[cd[e]f[]g]h").runWith(ReaderHandler(0) +! WriterHandler.strings.exec).mkString("\n")
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


  def writer = br ^ "Writer operations should work" ! {
    (for {
      _ <- Tell(1)
      _ <- Tell(2) *! Tell(3) *! Tell(4)
      _ <- Tell(5)
    } yield ())
    .runWith(WriterHandler.seq[Int].exec) must_== (1 to 5)
  }


  def except = br ^ "Error operations should work" ! {
    val missiles1 = Missiles()
    val missiles2 = Missiles()
    (for {
      i <- Return(123)
      _ <- Wrong("turn") *! missiles1.launch_!
      _ <- missiles2.launch_!
    } yield i)
    .runWith(ErrorHandler[String]) must_== Left("turn") and 
    missiles1.mustHaveLaunchedOnce and
    missiles2.mustNotHaveLaunched
  }


  def maybe = br ^ "Maybe operations should work" ! {
    val missiles1 = Missiles()
    val missiles2 = Missiles()
    (for {
      i <- Return(123)
      _ <- Naught *! missiles1.launch_!
      _ <- missiles2.launch_!
    } yield i)
    .runWith(MaybeHandler) must_== None and 
    missiles1.mustHaveLaunchedOnce and
    missiles2.mustNotHaveLaunched
  }


  def validation = br ^ "Validation operations should work" ! {
    val missiles1 = Missiles()
    val missiles2 = Missiles()
    (for {
      _ <- Invalid('x') *! missiles1.launch_! *! Invalid('y') *! Invalid('z')
      _ <- missiles2.launch_!
      _ <- Invalid('w')
    } yield ())
    .runWith(ValidationHandler[Char]) must_== Left(Vector('x', 'y', 'z')) and
    missiles1.mustHaveLaunchedOnce and
    missiles2.mustNotHaveLaunched
  }
}
