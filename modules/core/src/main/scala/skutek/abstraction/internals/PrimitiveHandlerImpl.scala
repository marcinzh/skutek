package skutek.abstraction.internals
import skutek.abstraction.!!
import skutek.abstraction.effect.{EffectId, FilterableEffect}


trait PrimitiveHandlerImpl extends PrimitiveHandler {
  private[abstraction] val effectId: EffectId

  def onSuspend[A, B, U](ma: A !! U, k: A => B !@! U): B !@! U

  def isParallel: Boolean
  final def isSequential = !isParallel
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
