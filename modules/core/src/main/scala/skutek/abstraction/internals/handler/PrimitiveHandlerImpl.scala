package skutek.abstraction.internals.handler
import skutek.abstraction._
import skutek.abstraction.ComputationCases.FilterFail
import skutek.abstraction.effect.FilterableEffect
import skutek.abstraction.internals.interpreter.Interpreter


object PrimitiveHandlerImpl {
  trait Filterable extends PrimitiveHandler {
    override type ThisEffect <: FilterableEffect
  }

  trait NonFilterable extends PrimitiveHandler {
    final override val onFail = None
  }

  trait Parallel extends PrimitiveHandler {
    final override def isParallel: Boolean = true
  }

  trait Sequential extends PrimitiveHandler {
    final override def isParallel: Boolean = false
  }

  trait Ultimate extends PrimitiveHandler {
    final override type !@![A, U] = Result[A]
    final override def onConceal[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = k(Interpreter.runPurish(ma))
    final override def onReveal[A, U](ma: A !@! U): Result[A] !! U = Return(ma)
  }

  trait Stateless extends PrimitiveHandler {
    final override type !@![A, U] = Result[A] !! U
    final override def onConceal[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = ma.flatMap(k)
    final override def onReveal[A, U](ma: A !@! U): Result[A] !! U = ma
  }

  trait Stateful[S] extends PrimitiveHandler {
    def initial: S
    final override type Result[A] = (A, S)
    final override type !@![A, U] = S => Result[A] !! U
    final override def onConceal[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = s => ma.flatMap(a => k(a)(s))
    final override def onReveal[A, U](ma: A !@! U): Result[A] !! U = ma(initial)

    final override def onReturn[A, U](a: A): A !@! U =
      s => Return((a, s))

    override def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      onProductDefault(ma, mb, k)

    final def onProductDefault[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => ma(s1).flatMap {
        case (a, s2) => mb(s2).flatMap {
          case (b, s3) => k((a, b))(s3)
        }
      }
  }
}
