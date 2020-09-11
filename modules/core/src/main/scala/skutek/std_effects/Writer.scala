package skutek.std_effects
import skutek.mwords._ //{Monoid, SingletonCons}
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect


trait Writer[W] extends Effect {
  case class Tell(value: W) extends Operation[Unit]
  def Tell[X](x: X)(implicit ev: SingletonCons[X, W]): Unit !! this.type = Tell(ev.singletonCons(x))

  def handler(implicit W: Monoid[W]) = DefaultWriterHandler[W, this.type](this).apply(W.empty)
}


object DefaultWriterHandler {
  def apply[W, Fx <: Writer[W]](fx: Fx)(implicit W: Monoid[W]) = new fx.Unary[W] with fx.Parallel {
    type Result[A] = (W, A)

    def onReturn[A, U](a: A): A !@! U =
      w => Return((w, a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Tell(w1) => w0 => k(())(w0 |@| w1)
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      w0 => (tma(W.empty) *! tmb(W.empty)).flatMap {
        case ((w1, a), (w2, b)) => k((a, b))(w0 |@| w1 |@| w2)
      }
  }
}
