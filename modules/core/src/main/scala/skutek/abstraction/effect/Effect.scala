package skutek.abstraction.effect


trait Effect {
  type ThisEffect = this.type
}

trait FilterableEffect extends Effect
