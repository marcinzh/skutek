package skutek.abstraction


trait Effect {
  type ThisEffect = this.type

  //@#@TODO add +! operator; add handler.handle(effect)(computation)
}

trait FilterableEffect extends Effect
