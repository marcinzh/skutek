package skutek.type_safety
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._
import org.specs2.execute._, Typecheck._
import org.specs2.matcher.TypecheckMatchers._


class InferenceTest extends Specification {

  case object Fx1 extends State[Double]
  case object Fx2 extends Writer[String]
  case object Fx3 extends Reader[Boolean]
  case object Fx4 extends Choice

  def is = {
    val eff = for {
      _ <- Return
      workaround <- Fx1.Get *! Fx3.Ask
      (a, b) = workaround
      _ <- Fx2.Tell("lies")
      c <- Fx4.Choose(1 to 10)
      // if c % 3 == 0
    } yield ()

    type Expected = Unit !! Fx1.type with Fx2.type with Fx3.type with Fx4.type

    typecheck {"implicitly[eff.type <:< Expected]"} must succeed
  }
}
