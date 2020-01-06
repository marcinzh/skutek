package skutek.abstraction.custom_effect
import skutek.abstraction._
import skutek.abstraction.ComputationCases._


trait PrimitiveHandler extends Handler {
  type ThisEffect <: Effect
  final override type Effects = ThisEffect
  val thisEffect: ThisEffect

  type !@![A, U]
  type Op[A] <: Operation[A, ThisEffect]

  def onReturn[A, U](a: A): A !@! U
  def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U
  def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U
  def onFail: Option[Op[Nothing]]
  def onConceal[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U
  def onReveal[A, U](ma: A !@! U): Result[A] !! U

  def isParallel: Boolean
  final def isSequential = !isParallel

  final override def interpret[A0, U](ma0: A0 !! U with Effects): Result[A0] !! U = {
    type UV = U with Effects
    def loopTramp[A](ma: A !! UV): A !@! U = onConceal(Return[U], (_: Any) => loop(ma))

    def loop[A](ma: A !! UV): A !@! U = ma match {
      case Return(a) => onReturn(a)
      case FlatMap(mx: Computation[tX, UV], k) =>
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
          case _ => onConceal(mx.asInstanceOf[X !! U], loopK)
        }
      case _ => loop(ma.map(a => a))
    }

    onReveal(loop(ma0))
  }
}
