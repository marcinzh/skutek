package skutek.abstraction.internals.interpreter
import skutek.abstraction.{!!, Return}
import skutek.abstraction.ComputationCases._
import skutek.abstraction.Computation
import skutek.abstraction.internals.handler.PrimitiveHandler


object Interpreter {
  @annotation.tailrec def runPure[A](ma: A !! Any): A =
    ma match {
      case Return(a) => a
      case FlatMap(mb, k) => mb match {
        case Return(b) => runPure(k(b))
        case FlatMap(mc, j) => runPure(mc.flatMap(c => j(c).flatMap(k)))
        case Product(mc, md) => runPure(mc.flatMap(c => md.flatMap(d => k((c, d)))))
        case _ => sys.error(s"Unhandled effect: $mb")
      }
      case _ => runPure(ma.map(a => a))
    }

  def runPurish[A, U](ma: A !! U): A = runPure(ma.downCast[Any])

  //@#@TODO
  trait EliminateEffect[V, F[_]] {
    def apply[A, U](ma: A !! U with V): F[A] !! U
  }

  //@#@TODO
  def runImpureTODO[H <: PrimitiveHandler](h: H): EliminateEffect[h.Effects, h.Result] =
    new EliminateEffect[h.Effects, h.Result] {
      def apply[A0, U](ma0: A0 !! U with h.Effects): h.Result[A0] !! U = {
        import h.!@!
        type UV = U with h.Effects
        def loopTramp[A](ma: A !! UV): A !@! U = h.onConceal(Return[U], (_: Any) => loop(ma))
        def loop[A](ma: A !! UV): A !@! U = ma match {
          case _ => ???
          //@#@TODO
        }

        h.onReveal(loop(ma0))
      }
    }

  def runImpure[A0, U, H <: PrimitiveHandler](h: H)(ma0: A0 !! U with h.Effects): h.Result[A0] !! U = {
    type UV = U with h.Effects
    type !@![A, V] = h.!@![A, V]
    def loopTramp[A](ma: A !! UV): A !@! U = h.onConceal(Return[U], (_: Any) => loop(ma))

    def loop[A](ma: A !! UV): A !@! U = ma match {
      case Return(a) => h.onReturn(a)
      case FlatMap(mx: Computation[tX, UV], k) =>
        type X = tX
        def loopK(x: X): A !@! U = loop(k(x))
        def loopTrampK(x: X): A !@! U = loopTramp(k(x))
        def operate(op: Operation[X, UV]): A !@! U = h.onOperation(op.asInstanceOf[h.Op[X]], loopTrampK(_: X))
        mx match {
          case Return(x) => loop(k(x))
          case FlatMap(mx, j) => loop(mx.flatMap(x => j(x).flatMap(k)))
          // case Product(my, mz) => h.onProduct(loopTramp(my), loopTramp(mz), loopK)
          case Product(my: Computation[tY, UV], mz: Computation[tZ, UV]) => h.onProduct(loopTramp(my), loopTramp(mz), loopK(_: (tY, tZ)))
          case op: Operation[X, UV] if op.thisEffect eq h.thisEffect => operate(op)
          case FilterFail if !(h.onFail eq None) => operate(h.onFail.get)
          // case _ => h.onConceal(mx.asInstanceOf[X !! U], loopK)
          case _ => h.onConceal(mx.asInstanceOf[X !! U], loopK(_: X))
        }
      case _ => loop(ma.map(a => a))
    }

    h.onReveal(loop(ma0))
  }
}
