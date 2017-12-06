package skutek
import _internals._

sealed trait Validation[T] 
object Validation extends EffectCompanion1[Validation]

case class Invalid[T](value: T) extends Operation[Nothing, Validation[T]]

case class ValidationHandler[T]() extends StatelessHandler.NoSecret[Validation[T]] {
  type Result[A] = Either[Vector[T], A]
  type Op[A] = Invalid[T]

  def onReturn[A](a: A) = Return(Right(a))

  def onOperation[A, B, U](op: Op[A], k: A => Result[B] !! U): Result[B] !! U = Return(Left(Vector(op.value)))

  def onProduct[A, B, C, U](effA: Result[A] !! U, effB: Result[B] !! U, k: ((A, B)) => Result[C] !! U): Result[C] !! U =
    (effA *! effB).flatMap { 
      case (Right(a), Right(b)) => k((a, b))
      case (Left(xs), Left(ys)) => Return(Left(xs ++ ys))
      case (Left(xs), _) => Return(Left(xs))
      case (_, Left(xs)) => Return(Left(xs))
    }
}
