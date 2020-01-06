package skutek.operations
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class WriterTest extends Specification {
  def is = {
    case object Fx extends Writer[Int]

    (for {
      _ <- Fx.Tell(1)
      _ <- Fx.Tell(2) *! Fx.Tell(3) *! Fx.Tell(4)
      _ <- Fx.Tell(5)
    } yield ())
    .runWith(Fx.handler.justState) must_== (1 to 5)
  }
}
