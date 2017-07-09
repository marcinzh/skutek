package skutek
import org.specs2._
import scala.util.Try 


class TagConflictTests extends Specification {

  def is = compo ^ local ^ reintro


  case object TagA
  case object TagB

  def compo = br ^ "Effect tag conflicts should be detected during composition of handlers" ! {

    val h1 = ReaderHandler(1)
    val h2 = ReaderHandler(2)
    val h_ = ReaderHandler("oops")

    List(
      Try { h1 +! h2                     } must beFailedTry,
      Try { h1 +! h_                     } must beFailedTry,
      Try { (h1 @! TagA) +! h2           } must beSuccessfulTry,
      Try { (h1 @! TagA) +! (h2 @! TagA) } must beFailedTry,
      Try { (h1 @! TagA) +! (h2 @! TagB) } must beSuccessfulTry
    ).reduce(_ and _)
  }


  def local = br ^ "Effect tag conflicts should be detected during local handling" ! {

    val goodEff = for {
      _ <- Ask[Double]
      _ <- Tell(true)
      _ <- Put("down")
    } yield ()

    val badEff = for {
      _ <- Ask[Double]
      _ <- Ask[String]
      _ <- Tell(true)
      _ <- Put("down")
    } yield ()

    val r = ReaderHandler(0.0)
    val w = WriterHandler.seq[Boolean]

    List(
      Try { r.fx[State[String]].fx[Writer[Boolean]].handle(goodEff)                   } must beSuccessfulTry,
      Try { r.fx[State[String]].fx[Maybe].fx[Writer[Boolean]].handle(goodEff)         } must beSuccessfulTry,
      Try { r.fx[State[String]].fx[State[Int]].fx[Writer[Boolean]].handle(goodEff)    } must beFailedTry,
      Try { r.fx[State[String]].fx[Reader[String]].fx[Writer[Boolean]].handle(badEff) } must beFailedTry,
      Try { w.fx[State[String]].fx[Reader[Double]].fx[Reader[String]].handle(badEff)  } must beFailedTry,

      Try { goodEff.fx[State[String]].fx[Writer[Boolean]].handleWith(r)                   } must beSuccessfulTry,
      Try { goodEff.fx[State[String]].fx[Maybe].fx[Writer[Boolean]].handleWith(r)         } must beSuccessfulTry,
      Try { goodEff.fx[State[String]].fx[State[Int]].fx[Writer[Boolean]].handleWith(r)    } must beFailedTry,
      Try { badEff.fx[State[String]].fx[Reader[String]].fx[Writer[Boolean]].handleWith(r) } must beFailedTry,
      Try { badEff.fx[State[String]].fx[Reader[Double]].fx[Reader[String]].handleWith(w)  } must beFailedTry
    ).reduce(_ and _)
  }


  def reintro = br ^ "Reintroduction of already locally handled effect, should not cause tag conflict" ! {

    Try { 
      val eff1 = for {
        a <- Ask[Int]
        i <- Get[Int]
        _ <- Put(i * a)
      } yield ()

      val eff2 = StateHandler(2).exec.fx[Reader[Int]].handle(eff1)

      val eff3 = for {
        i <- eff2
        s <- Get[String]
        _ <- Put(s(i).toString)
      } yield ()

      (StateHandler((0 to 9).mkString).exec +! ReaderHandler(3)).run(eff3)

    } must beSuccessfulTry.withValue("6")
  }
}
