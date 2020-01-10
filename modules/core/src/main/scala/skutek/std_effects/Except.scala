package skutek.std_effects
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect


trait Except[E] extends Effect {
  case class Raise(value: E) extends Operation[Nothing]

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Raise(e)
    }

  val handler = DefaultExceptHandler.handler[E, this.type](this)
  val handlerShort = DefaultExceptHandler.handlerShort[E, this.type](this)
}


object DefaultExceptHandler {
  def handler[E, Fx <: Except[E]](fx: Fx) = new fx.Nullary with fx.Sequential {
    type Result[A] = Either[E, A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Raise(e) => Return(Left(e))
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }


  def handlerShort[E, Fx <: Except[E]](fx: Fx) = new fx.Nullary with fx.Sequential {
    type Result[A] = Either[E, A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Raise(e) => Return(Left(e))
      }

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
