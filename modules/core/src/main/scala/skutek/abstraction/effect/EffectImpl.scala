package skutek.abstraction.effect
import skutek.abstraction.ComputationCases.{Operation => AbstractOp}
import skutek.abstraction.{HandlerCases => HC}
import skutek.abstraction.internals.handler.PrimitiveHandlerImpl
import skutek.abstraction.internals.handler.{PrimitiveHandlerImpl => PHI}


protected sealed trait CommonEffectImpl extends Effect { outer =>
  trait Operation[A] extends AbstractOp[A, ThisEffect] {
    final override def effectId: EffectId = outer
  }

  trait ThisHandler extends PrimitiveHandlerImpl {
    final override type Operation[A] = outer.Operation[A]
    final override type ThisEffect = outer.ThisEffect
    final override val effectId: EffectId = outer
  }
}

trait EffectImpl extends CommonEffectImpl {
  trait ThisHandler extends super.ThisHandler with PHI.NonFilterable

  trait Parallel extends PHI.Parallel with ThisHandler
  trait Sequential extends PHI.Sequential with ThisHandler

  trait Nullary extends HC.Nullary with ThisHandler
  trait Unary[S] extends HC.Unary[S] with ThisHandler
  trait Foreign extends HC.Foreign with ThisHandler
}

trait FilterableEffectImpl extends CommonEffectImpl with FilterableEffect {
  trait ThisHandler extends super.ThisHandler with PHI.Filterable

  trait Parallel extends PHI.Parallel with ThisHandler
  trait Sequential extends PHI.Sequential with ThisHandler

  trait Nullary extends HC.Nullary with ThisHandler
  trait Unary[S] extends HC.Unary[S] with ThisHandler
  trait Foreign extends HC.Foreign with ThisHandler
}
