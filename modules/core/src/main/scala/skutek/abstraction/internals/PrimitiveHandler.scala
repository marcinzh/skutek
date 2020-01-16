package skutek.abstraction.internals
import skutek.abstraction.{!!, HandlerStub}
import skutek.abstraction.ComputationCases.{Operation => AbstractOp}
import skutek.abstraction.effect.{EffectId, AnyEffect, Effect}


trait PrimitiveHandler extends HandlerStub {
  private[abstraction] val effectId: EffectId

  type ThisEffect <: AnyEffect
  final override type Effects = ThisEffect

  type !@![A, U]
  type Operation[A] <: AbstractOp[A, ThisEffect]

  def onReturn[A, U](a: A): A !@! U
  def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U
  def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U
  def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U
  def onFail: Option[Operation[Nothing]]

  def isParallel: Boolean
  final def isSequential = !isParallel
}


object PrimitiveHandler {
  trait Filterable extends PrimitiveHandler {
    override type ThisEffect <: Effect.Filterable
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

  trait Nullary extends PrimitiveHandler {
    final override type !@![A, U] = Result[A] !! U
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = ma.flatMap(k)
  }

  trait Unary[S] extends PrimitiveHandler {
    final override type !@![A, U] = S => Result[A] !! U
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = s => ma.flatMap(a => k(a)(s))
  }

  trait Foreign extends PrimitiveHandler {
    final override type !@![A, U] = Result[A]
    final override def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U = k(Interpreter.runPure(ma))
  }
}
