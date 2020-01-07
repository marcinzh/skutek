package skutek.abstraction.internals.handler
import skutek.abstraction.{!!, HandlerCases, ComputationCases}
import skutek.abstraction.{Effect}
import skutek.abstraction.internals.interpreter.Interpreter


trait PrimitiveHandler extends HandlerCases.Unsealed {
  type ThisEffect <: Effect
  final override type Effects = ThisEffect
  val thisEffect: ThisEffect

  type !@![A, U]
  type Op[A] <: ComputationCases.Operation[A, ThisEffect]

  def onReturn[A, U](a: A): A !@! U
  def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U
  def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U
  def onFail: Option[Op[Nothing]]
  def onConceal[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U
  def onReveal[A, U](ma: A !@! U): Result[A] !! U

  def isParallel: Boolean
  final def isSequential = !isParallel

  final override def interpret[A, U](ma: A !! U with Effects): Result[A] !! U = Interpreter.runImpure[A, U, this.type](this)(ma)
}
