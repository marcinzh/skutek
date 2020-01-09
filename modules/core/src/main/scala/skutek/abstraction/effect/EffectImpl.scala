package skutek.abstraction.effect
import skutek.abstraction.ComputationCases
import skutek.abstraction.{HandlerCases => HC}
import skutek.abstraction.internals.handler.PrimitiveHandlerImpl
import skutek.abstraction.internals.handler.{PrimitiveHandlerImpl => HI}


protected sealed trait CommonEffectImpl extends Effect { outer =>
  trait Op[A] extends ComputationCases.Operation[A, ThisEffect] {
    final override def thisEffect: ThisEffect = outer
  }

  trait ThisHandler extends PrimitiveHandlerImpl {
    final override type Op[A] = outer.Op[A]
    final override type ThisEffect = outer.ThisEffect
    final override val thisEffect: ThisEffect = outer
  }
}

trait EffectImpl extends CommonEffectImpl {
  trait ThisHandler extends super.ThisHandler with HI.NonFilterable

  trait Parallel extends HI.Parallel with ThisHandler
  trait Sequential extends HI.Sequential with ThisHandler

  trait Nullary extends HC.Nullary with ThisHandler
  trait Unary[S] extends HC.Unary[S] with ThisHandler
  trait Foreign extends HC.Foreign with ThisHandler
}

trait FilterableEffectImpl extends CommonEffectImpl with FilterableEffect {
  trait ThisHandler extends super.ThisHandler with HI.Filterable

  trait Parallel extends HI.Parallel with ThisHandler
  trait Sequential extends HI.Sequential with ThisHandler

  trait Nullary extends HC.Nullary with ThisHandler
  trait Unary[S] extends HC.Unary[S] with ThisHandler
  trait Foreign extends HC.Foreign with ThisHandler
}
