package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.custom_effect._
import skutek.utils.Accumulator


trait Validation[E] extends EffectImpl with AccumulatingEffect[E] {
  case class Invalid(value: E) extends Op[Nothing]

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Invalid(e)
    }

  final override type HandlerCtor[S] = CommonHandler[S]
  final override def handlerCtor[S](acc: Accumulator[E, S]) = new CommonHandler[S](acc)

  class CommonHandler[S](acc: Accumulator[E, S]) extends Stateless with Parallel {
    final override type Result[A] = Either[S, A]

    final override def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    final override def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Invalid(e) => Return(Left(acc.one(e)))
      }

    final override def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e1), Left(e2)) => Return(Left(acc.add(e1, e2)))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }
}
