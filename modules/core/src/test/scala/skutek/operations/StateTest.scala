package skutek.operations
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class StateTests extends Specification {
  def is = {
    case object Fx extends State[Int]

    (for {
      a <- Fx.Get
      _ <- Fx.Put(a + 99)
      _ <- Fx.Put(a + 10)
      b <- Fx.Get
      _ <- Fx.Put(a + 999)
      _ <- Fx.Put(a + 100)
      c <- Fx.Get
      _ <- Fx.Put(a + 9999)
      _ <- Fx.Put(c + 1000)
    } yield ())
    .runWith(Fx.handler(1).exec) must_== 1101
  }
}
