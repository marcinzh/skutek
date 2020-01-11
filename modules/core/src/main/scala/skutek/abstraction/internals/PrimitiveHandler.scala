package skutek.abstraction.internals
import skutek.abstraction.HandlerStub
import skutek.abstraction.ComputationCases.{Operation => AbstractOp}
import skutek.abstraction.effect.AnyEffect


trait PrimitiveHandler extends HandlerStub {
  type ThisEffect <: AnyEffect
  final override type Effects = ThisEffect

  type !@![A, U]
  type Operation[A] <: AbstractOp[A, ThisEffect]

  def onReturn[A, U](a: A): A !@! U
  def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U
  def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U
  def onFail: Option[Operation[Nothing]]
}
