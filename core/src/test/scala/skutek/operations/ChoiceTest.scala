package skutek.operations
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class ChoiceTest extends Specification {
  def is = List(wog, wg).reduce(_ and _)

  def wog = {
    case object Fx extends Choice

    val eff = for {
      i <- Fx.Choose(1 to 2)
      c <- Fx.Choose('a' to 'b')
    } yield s"$i$c"

    List(
      eff.runWith(Fx.findAll) must_== Vector("1a", "1b", "2a", "2b"),
      eff.runWith(Fx.findFirst) must_== Some("1a")
    ).reduce(_ and _)
  }


  def wg = {
    case object Fx extends Choice

    val eff = for {
      i <- Fx.Choose(0 to 3)
      if i % 2 != 0
      c <- Fx.Choose('a' to 'c')
    } yield s"$i$c"

    List(
      eff.runWith(Fx.findAll) must_== Vector("1a", "1b", "1c", "3a", "3b", "3c"),
      eff.runWith(Fx.findFirst) must_== Some("1a")
    ).reduce(_ and _)
  }
}
