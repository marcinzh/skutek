package skutek.abstraction.internals
import skutek.abstraction.{!!, Return}
import skutek.abstraction.ComputationCases.{Operation => AbstractOp}
import skutek.abstraction.ComputationCases._


trait Interpreter[U, V, !@![_, _]] {
  def apply[A](ma: A !! U with V): A !@! U
}


object Interpreter {
  @annotation.tailrec def runPure[A, U](ma: A !! U): A =
    ma match {
      case Return(a) => a
      case FlatMap(mb, k) => mb match {
        case Return(b) => runPure(k(b))
        case FlatMap(mb, j) => runPure(mb.flatMap(b => j(b).flatMap(k)))
        case Product(mb, mc) => runPure(mb.flatMap(b => mc.flatMap(c => k((b, c)))))
        case _ => sys.error(s"Unhandled effect: $mb")
      }
      case _ => runPure(ma.map(a => a))
    }
}


trait WithDefaultInterpreter extends PrimitiveHandler {
  type ThisInterpreter[U] = Interpreter[U, Effects, !@!]
  final def interpreter[U]: ThisInterpreter[U] = interpreterAny.asInstanceOf[ThisInterpreter[U]]
  private val interpreterAny: ThisInterpreter[Any] = makeInterpreter[Any]

  private final def makeInterpreter[U]: ThisInterpreter[U] = {
    type ThisOp[A] = Operation[A]
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
          case Fail if !(onFail eq None) => operate(onFail.get)
          case _ => onSuspend(mx.asInstanceOf[X !! U], loopK)
        }
      case _ => loop(ma.map(a => a))
    }

    new ThisInterpreter[U] {
      def apply[A](ma: A !! U with Effects): A !@! U = loop(ma)
    }
  }
}


trait WithUnaryInterpreter[S] extends PrimitiveHandler.Unary[S] {
  type UnaryInterpreter[U] = Interpreter[U, Effects, Lambda[(X, V) => Result[X] !! U]]
  final def unaryInterpreter[U]: S => UnaryInterpreter[U] = unaryInterpreterAny.asInstanceOf[S => UnaryInterpreter[U]]
  private val unaryInterpreterAny: S => UnaryInterpreter[Any] = makeUnaryInterpreter[Any]

  //// Default interpreter would do, but for some mysterious reason, this version runs noticably faster
  private final def makeUnaryInterpreter[U]: S => UnaryInterpreter[U] = {
    type ThisOp[A] = Operation[A]
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
          case Fail if !(onFail eq None) => operate(onFail.get, s)
          case _ => onSuspend(mx.asInstanceOf[X !! U], loopK)(s)
        }
      case _ => loop(ma.map(a => a), s)
    }

    s => new UnaryInterpreter[U] {
      def apply[A](ma: A !! U with Effects): Result[A] !! U = loop(ma, s)
    }
  }
}
