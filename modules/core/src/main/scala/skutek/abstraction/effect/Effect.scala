package skutek.abstraction.effect
import skutek.abstraction.ComputationCases.{Operation => AbstractOp}
import skutek.abstraction.{HandlerCases => HC}
import skutek.abstraction.internals.PrimitiveHandler
import skutek.abstraction.internals.{PrimitiveHandler => PH}


sealed trait EffectId


private[skutek] sealed trait AnyEffect extends EffectId { outer =>
  type ThisEffect = this.type

  trait Operation[A] extends AbstractOp[A, ThisEffect] {
    final override def effectId: EffectId = outer
  }

  trait ThisHandler extends PrimitiveHandler {
    final override type Operation[A] = outer.Operation[A]
    final override type ThisEffect = outer.ThisEffect
    final override val effectId: EffectId = outer
  }
}


trait Effect extends AnyEffect {
  trait ThisHandler extends super.ThisHandler with PH.NonFilterable

  trait Parallel extends ThisHandler with PH.Parallel
  trait Sequential extends ThisHandler with PH.Sequential

  trait Nullary extends ThisHandler with HC.Nullary
  trait Unary[S] extends ThisHandler with HC.Unary[S]
  trait Foreign extends ThisHandler with HC.Foreign
}


trait FailEffect


object Effect {
  trait Filterable extends AnyEffect with FailEffect {
    trait ThisHandler extends super.ThisHandler with PH.Filterable

    trait Parallel extends ThisHandler with PH.Parallel
    trait Sequential extends ThisHandler with PH.Sequential

    trait Nullary extends ThisHandler with HC.Nullary
    trait Unary[S] extends ThisHandler with HC.Unary[S]
    trait Foreign extends ThisHandler with HC.Foreign
  }
}
