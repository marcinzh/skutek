package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.custom_effect._


trait Validation[E] extends EffectImpl {
  case class Invalid(value: E) extends Op[Nothing]

  def from[A](ma: Either[E, A]): A !! ThisEffect = 
    ma match {
      case Right(a) => Return(a)
      case Left(e) => Invalid(e)
    }

  trait CommonHandler[T] extends Stateless with Parallel {
    def one(e: E): T
    def add(acc1: T, acc2: T): T

    final type Result[A] = Either[T, A]

    final def onReturn[A, U](a: A): A !@! U =
      Return(Right(a))

    final def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Invalid(e) => Return(Left(one(e)))
      }

    final def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (Right(a), Right(b)) => k((a, b))
        case (Left(e1), Left(e2)) => Return(Left(add(e1, e2)))
        case (Left(e), _) => Return(Left(e))
        case (_, Left(e)) => Return(Left(e))
      }
  }

  def handler = intoSeq

  def intoSeq = new CommonHandler[Vector[E]] {
    def one(e: E) = Vector(e)
    def add(es1: Vector[E], es2: Vector[E]) = es1 ++ es2
  }

  def reduce(op: (E, E) => E) = new CommonHandler[E] {
    def one(e: E) = e
    def add(e1: E, e2: E) = op(e1, e2)
  }
}
