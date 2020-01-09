package skutek.std_effects
import mwords._ //{Monoid, SingletonCons}
import skutek.abstraction._
import skutek.abstraction.effect._


trait Validation[E] extends Effect {
  case class Invalid(value: E) extends Operation[Nothing]
  def Invalid[X](x: X)(implicit ev: SingletonCons[X, E]): Nothing !! this.type = Invalid(ev.singletonCons(x))

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Invalid(e)
    }

  def handler(implicit E: Monoid[E]) = new DefaultHandler

  class DefaultHandler(implicit E: Monoid[E]) extends Nullary with Parallel {
    final override type Result[A] = Either[E, A]

    final override def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    final override def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Invalid(e) => Return(Left(e))
      }

    final override def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e1), Left(e2)) => Return(Left(e1 |@| e2))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }
}
