package skutek.std_effects
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect


trait Reader[R] extends Effect {
  case object Ask extends Operation[R]

  def Asks[A](f: R => A) = Ask.map(f)
  def Local[A, U](r: R)(scope: A !! U) = LocalMod(_ => r)(scope)
  def LocalMod[A, U](f: R => R)(scope: A !! U) = Ask.flatMap(r => handler(f(r)).handle[U](scope))

  def handler(r: R) = DefaultReaderHandler[R, this.type](this, r)
}


object DefaultReaderHandler {
  def apply[R, Fx <: Reader[R]](fx: Fx, r: R) = new fx.Nullary with fx.Parallel {
    type Result[A] = A

    def onReturn[A, U](a: A): A !@! U =
      Return(a)

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Ask => k(r)
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap(k)
  }
}
