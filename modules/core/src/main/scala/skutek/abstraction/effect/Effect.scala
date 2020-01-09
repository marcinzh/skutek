package skutek.abstraction.effect


sealed trait EffectId

trait Effect extends EffectId {
  type ThisEffect = this.type
}

trait FilterableEffect extends Effect
