package skutek
import _internals._

sealed trait Error[T] 
object Error extends EffectCompanion1[Error]

case class Wrong[T](value: T) extends Operation[Nothing, Error[T]]

case class ErrorHandler[T]() extends StatelessHandler.NoSecret[Error[T]] {
  type Result[A] = Either[T, A]
  type Op[A] = Wrong[T]

  def onReturn[A](a: A) = Return(Right(a))

  def onOperation[A, B, U](op: Op[A], k: A => Result[B] !! U): Result[B] !! U = Return(Left(op.value))
  
  def onProduct[A, B, C, U](effA: Result[A] !! U, effB: Result[B] !! U, k: ((A, B)) => Result[C] !! U): Result[C] !! U =
    (effA *! effB).flatMap { 
      case (Right(a), Right(b)) => k((a, b))
      case (Left(x), _) => Return(Left(x))
      case (_, Left(x)) => Return(Left(x))
    }
}


trait Error_exports {
  implicit class EitherToComputation[L, R](thiz: Either[L, R]) {
    def toEff: R !! Error[L] = thiz match {
      case Right(x) => Return(x)
      case Left(x) => Wrong(x)
    }
  }

  import scala.util._
  implicit class TryToComputation[A](thiz: Try[A]) {
    def toEff: A !! Error[Throwable] = thiz match {
      case Success(x) => Return(x)
      case Failure(x) => Wrong(x)
    }
  }
}
