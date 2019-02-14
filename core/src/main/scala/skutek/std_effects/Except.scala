package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.custom_effect._


trait Except[E] extends EffectImpl {
  case class Raise(value: E) extends Op[Nothing]

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Raise(e)
    }

  trait CommonHandler extends Stateless {
    type Result[A] = Either[E, A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Raise(e) => Return(Left(e))
      }
  }

  def handler = new CommonHandler with Parallel {
    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }

  def handlerShort = new CommonHandler with Sequential {
    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      ma.flatMap {
        case Right(a) => mb.flatMap {
          case Right(b) => k((a, b))
          case Left(e) => Return(Left(e))
        }
        case Left(e) => Return(Left(e))
      }
  }
}