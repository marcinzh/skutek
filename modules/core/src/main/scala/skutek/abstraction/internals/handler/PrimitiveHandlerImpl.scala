package skutek.abstraction.internals.handler
import skutek.abstraction.{!!, Return, ComputationCases}
import skutek.abstraction.effect.FilterableEffect


trait Interpreter[U, V, !@![_, _]] {
  def apply[A](ma: A !! U with V): A !@! U
}


trait PrimitiveHandlerImpl extends PrimitiveHandler {
  //@#@TODO backport effectId
  private[abstraction] val thisEffect: ThisEffect

  def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U

  def isParallel: Boolean
  final def isSequential = !isParallel

  //=========================

  type ThisInterpreter[U] = Interpreter[U, Effects, !@!]
  final def interpreter[U]: ThisInterpreter[U] = interpreterAny.asInstanceOf[ThisInterpreter[U]]
  private val interpreterAny: ThisInterpreter[Any] = makeInterpreter[Any]

  private final def makeInterpreter[U]: ThisInterpreter[U] = {
    import ComputationCases._
    type UV = U with Effects
    def loopTramp[A](ma: A !! UV): A !@! U = onSuspend(Return[U], (_: Any) => loop(ma))

    def loop[A](ma: A !! UV): A !@! U = ma match {
      case Return(a) => onReturn(a)
      case FlatMap(mx: !![tX, UV], k) =>
        type X = tX
        def loopK(x: X): A !@! U = loop(k(x))
        def loopTrampK(x: X): A !@! U = loopTramp(k(x))
        def operate(op: Operation[X, UV]): A !@! U = onOperation(op.asInstanceOf[Op[X]], loopTrampK)
        mx match {
          case Return(x) => loop(k(x))
          case FlatMap(mx, j) => loop(mx.flatMap(x => j(x).flatMap(k)))
          case Product(my, mz) => onProduct(loopTramp(my), loopTramp(mz), loopK)
          case op: Operation[X, UV] if op.thisEffect eq thisEffect => operate(op)
          case FilterFail if !(onFail eq None) => operate(onFail.get)
          case _ => onSuspend(mx.asInstanceOf[X !! U], loopK)
        }
      case _ => loop(ma.map(a => a))
    }

    new ThisInterpreter[U] {
      def apply[A](ma: A !! U with Effects): A !@! U = loop(ma)
    }
  }
}


object PrimitiveHandlerImpl {
  trait Filterable extends PrimitiveHandlerImpl {
    override type ThisEffect <: FilterableEffect
  }

  trait NonFilterable extends PrimitiveHandlerImpl {
    final override val onFail = None
  }

  trait Parallel extends PrimitiveHandlerImpl {
    final override def isParallel: Boolean = true
  }

  trait Sequential extends PrimitiveHandlerImpl {
    final override def isParallel: Boolean = false
  }

  trait Nullary extends PrimitiveHandlerImpl {
    final override type !@![A, U] = Result[A] !! U
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = ma.flatMap(k)
  }

  trait Unary[S] extends PrimitiveHandlerImpl {
    final override type !@![A, U] = S => Result[A] !! U
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = s => ma.flatMap(a => k(a)(s))
  }

  trait Foreign extends PrimitiveHandlerImpl {
    final override type !@![A, U] = Result[A]
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = k(ma.runPure)
  }
}
