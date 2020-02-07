package skutek.std_effects
import skutek.mwords._ //{Semigroup, SingletonCons}
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect


trait Validation[E] extends Effect {
  case class Invalid(value: E) extends Operation[Nothing]
  def Invalid[X](x: X)(implicit ev: SingletonCons[X, E]): Nothing !! this.type = Invalid(ev.singletonCons(x))

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Invalid(e)
    }

  def handler(implicit E: Semigroup[E]) = DefaultValidationHandler[E, this.type](this)
}


object DefaultValidationHandler {
  def apply[E: Semigroup, Fx <: Validation[E]](fx: Fx) = new fx.Nullary with fx.Parallel {
    final override type Result[A] = Either[E, A]

    final override def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    final override def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Invalid(e) => Return(Left(e))
      }

    final override def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e1), Left(e2)) => Return(Left(e1 |@| e2))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }
}
