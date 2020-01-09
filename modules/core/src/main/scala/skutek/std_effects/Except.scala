package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait Except[E] extends Effect {
  case class Raise(value: E) extends Operation[Nothing]

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Raise(e)
    }

  trait CommonHandler extends Nullary {
    final override type Result[A] = Either[E, A]

    final override def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    final override def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Raise(e) => Return(Left(e))
      }
  }

  def handler = new CommonHandler with Parallel {
    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }

  def handlerShort = new CommonHandler with Sequential {
    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      tma.flatMap {
        case Right(a) => tmb.flatMap {
          case Right(b) => k((a, b))
          case Left(e) => Return(Left(e))
        }
        case Left(e) => Return(Left(e))
      }
  }
}
