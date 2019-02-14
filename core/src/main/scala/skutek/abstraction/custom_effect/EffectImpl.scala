package skutek.abstraction.custom_effect
import skutek.abstraction._
import skutek.abstraction.custom_effect.{PrimitiveHandlerImpl => H}


protected sealed trait CommonEffectImpl extends Effect { outer =>
  trait Op[A] extends Computation.Operation[A, ThisEffect] {
    final override def thisEffect: ThisEffect = outer
  }

  trait ThisHandler extends PrimitiveHandler {
    final override type Op[A] = outer.Op[A]
    final override type ThisEffect = outer.ThisEffect
    final override val thisEffect: ThisEffect = outer
  }
}

trait EffectImpl extends CommonEffectImpl { outer =>
  trait ThisHandler extends super.ThisHandler with H.NonFilterable

  trait Parallel extends H.Parallel with ThisHandler
  trait Sequential extends H.Sequential with ThisHandler

  trait Stateless extends H.Stateless with ThisHandler
  trait Stateful[S] extends H.Stateful[S] with ThisHandler
  trait Stateful2[S] extends H.Stateful2[S] with ThisHandler
  trait Ultimate extends H.Ultimate with ThisHandler
}

trait FilterableEffectImpl extends CommonEffectImpl with FilterableEffect { outer =>
  trait ThisHandler extends super.ThisHandler with H.Filterable

  trait Parallel extends H.Parallel with ThisHandler
  trait Sequential extends H.Sequential with ThisHandler

  trait Stateless extends H.Stateless with ThisHandler
  trait Stateful[S] extends H.Stateful[S] with ThisHandler
  trait Stateful2[S] extends H.Stateful2[S] with ThisHandler
  trait Ultimate extends H.Ultimate with ThisHandler
}
