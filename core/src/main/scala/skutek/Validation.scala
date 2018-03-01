package skutek
import _internals._

sealed trait Validation[T] 
object Validation extends EffectCompanion1[Validation]

case class Invalid[T](value: T) extends Operation[Nothing, Validation[T]]

case class ValidationHandler[T]() extends StatelessHandler[Validation[T]] {
  type Result[A] = Either[Vector[T], A]
  type Op[A] = Invalid[T]

  def onReturn[A](a: A): Secret[A, Any] = Return(Right(a))

  def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U] = Return(Left(Vector(op.value)))
  
  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U] =
    (aa *! bb).flatMap { 
      case (Right(a), Right(b)) => k((a, b))
      case (Left(xs), Left(ys)) => Return(Left(xs ++ ys))
      case (Left(xs), _) => Return(Left(xs))
      case (_, Left(xs)) => Return(Left(xs))
    }
}
