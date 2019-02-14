package skutek.operations
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class ValidationTest extends Specification with CanLaunchTheMissiles {
  def is = {
    case object Fx extends Validation[Char]

    val missile1 = Missile()
    val missile2 = Missile()
    (for {
      _ <- Fx.Invalid('x') *! missile1.launch_! *! Fx.Invalid('y') *! Fx.Invalid('z')
      _ <- missile2.launch_!
      _ <- Fx.Invalid('w')
    } yield ())
    .runWith(Fx.handler) must_== Left(Vector('x', 'y', 'z')) and
    missile1.mustHaveLaunchedOnce and
    missile2.mustNotHaveLaunched
  }
}
