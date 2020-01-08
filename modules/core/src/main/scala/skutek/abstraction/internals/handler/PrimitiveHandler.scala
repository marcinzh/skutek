package skutek.abstraction.internals.handler
import mwords.{MonadPar, Functor, Identity, ~>}
import skutek.abstraction.{!!, HandlerStub}
import skutek.abstraction.{Computation, Return}
import skutek.abstraction.ComputationCases
import skutek.abstraction.effect.Effect


trait Interpreter[U, V, !@![_, _]] {
  def apply[A](ma: A !! U with V): A !@! U
}

trait PrimitiveHandler extends HandlerStub {
  final override type Effects = ThisEffect
  type ThisEffect <: Effect
  val thisEffect: ThisEffect

  type !@![A, U]
  type Result[A]
  type Op[A] <: ComputationCases.Operation[A, ThisEffect]

  def onReturn[A, U](a: A): A !@! U
  def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U
  def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U
  def onFail: Option[Op[Nothing]]
  def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U

  def isParallel: Boolean
  final def isSequential = !isParallel

  type ThisInterpreter[U] = Interpreter[U, Effects, !@!]
  protected[abstraction] final def interpreter[U]: ThisInterpreter[U] = interpreterAny.asInstanceOf[ThisInterpreter[U]]
  private val interpreterAny: ThisInterpreter[Any] = makeInterpreter[Any]
  def makeInterpreter[U]: ThisInterpreter[U]
}


// object PrimitiveHandler {
// }
