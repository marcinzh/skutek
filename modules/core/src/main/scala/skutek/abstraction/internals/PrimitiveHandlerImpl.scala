package skutek.abstraction.internals
import skutek.abstraction.{!!, Return, ComputationCases}
import skutek.abstraction.ComputationCases.{Operation => AbstractOp}
import skutek.abstraction.effect.{EffectId, FilterableEffect}


trait Interpreter[U, V, !@![_, _]] {
  def apply[A](ma: A !! U with V): A !@! U
}


trait PrimitiveHandlerImpl extends PrimitiveHandler {
  private[abstraction] val effectId: EffectId

  def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U

  def isParallel: Boolean
  final def isSequential = !isParallel

  //=========================

  type ThisInterpreter[U] = Interpreter[U, Effects, !@!]
  final def interpreter[U]: ThisInterpreter[U] = interpreterAny.asInstanceOf[ThisInterpreter[U]]
  private val interpreterAny: ThisInterpreter[Any] = makeInterpreter[Any]

  private final def makeInterpreter[U]: ThisInterpreter[U] = {
    type ThisOp[A] = Operation[A]
    import ComputationCases._
    type UV = U with Effects
    def loopTramp[A](ma: A !! UV): A !@! U = onSuspend(Return[U], (_: Any) => loop(ma))

    def loop[A](ma: A !! UV): A !@! U = ma match {
      case Return(a) => onReturn(a)
      case FlatMap(mx: !![tX, UV], k) =>
        type X = tX
        def loopK(x: X): A !@! U = loop(k(x))
        def loopTrampK(x: X): A !@! U = loopTramp(k(x))
        def operate(op: AbstractOp[X, UV]): A !@! U = onOperation(op.asInstanceOf[ThisOp[X]], loopTrampK)
        mx match {
          case Return(x) => loop(k(x))
          case FlatMap(mx, j) => loop(mx.flatMap(x => j(x).flatMap(k)))
          case Product(my, mz) => onProduct(loopTramp(my), loopTramp(mz), loopK)
          case op: AbstractOp[X, UV] if op.effectId eq effectId => operate(op)
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

    //=========================

    type UnaryInterpreter[U] = Interpreter[U, Effects, Lambda[(X, V) => Result[X] !! U]]
    final def unaryInterpreter[U]: S => UnaryInterpreter[U] = unaryInterpreterAny.asInstanceOf[S => UnaryInterpreter[U]]
    private val unaryInterpreterAny: S => UnaryInterpreter[Any] = makeUnaryInterpreter[Any]

    //// The defaultInterpreter would do, but for some mysterious reason, this version runs noticably faster
    private final def makeUnaryInterpreter[U]: S => UnaryInterpreter[U] = {
      type ThisOp[A] = Operation[A]
      import ComputationCases._
      type UV = U with Effects
      def loopTramp[A](ma: A !! UV)(s: S): Result[A] !! U = onSuspend[Any, A, U](Return[U], _ => loop(ma, _))(s)

      def loop[A](ma: A !! UV, s: S): Result[A] !! U = ma match {
        case Return(a) => onReturn(a)(s)
        case FlatMap(mx: !![tX, UV], k) =>
          type X = tX
          def loopK(x: X): A !@! U = loop(k(x), _)
          def loopTrampK(x: X): A !@! U = loopTramp(k(x))
          def operate(op: AbstractOp[X, UV], s: S): Result[A] !! U = onOperation[X, A, U](op.asInstanceOf[ThisOp[X]], loopTrampK)(s)
          mx match {
            case Return(x) => loop(k(x), s)
            case FlatMap(mx, j) => loop(mx.flatMap(x => j(x).flatMap(k)), s)
            case Product(my, mz) => onProduct(loopTramp(my), loopTramp(mz), loopK)(s)
            case op: AbstractOp[X, UV] if op.effectId eq effectId => operate(op, s)
            case FilterFail if !(onFail eq None) => operate(onFail.get, s)
            case _ => onSuspend(mx.asInstanceOf[X !! U], loopK)(s)
          }
        case _ => loop(ma.map(a => a), s)
      }

      s => new UnaryInterpreter[U] {
        def apply[A](ma: A !! U with Effects): Result[A] !! U = loop(ma, s)
      }
    }
  }

  trait Foreign extends PrimitiveHandlerImpl {
    final override type !@![A, U] = Result[A]
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = k(ma.runPure)
  }
}
