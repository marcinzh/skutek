package skutek.std_effects
import mwords._ //{Monoid, SingletonCons}
import skutek.abstraction._
import skutek.abstraction.effect._


trait Writer[W] extends EffectImpl {
  case class Tell(value: W) extends Op[Unit]
  def Tell[X](x: X)(implicit ev: SingletonCons[X, W]): Unit !! this.type = Tell(ev.singletonCons(x))

  def handler(implicit W: Monoid[W]) = new Nullary with Parallel {
    type Result[A] = (A, W)

    def onReturn[A, U](a: A): A !@! U =
      Return((a, Monoid[W].empty))

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Tell(w1) =>
          k(()).map {
            case (b, w2) => (b, w1 |@| w2)
          }
      }

    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case ((a, w1), (b, w2)) =>
          val w12 = w1 |@| w2
          k((a, b)).map {
            case (c, w3) =>
              (c, w12 |@| w3)
          }
      }
  }
}
