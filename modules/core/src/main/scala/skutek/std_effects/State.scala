package skutek.std_effects
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect


trait State[S] extends Effect {
  case object Get extends Operation[S]
  case class Put(value: S) extends Operation[Unit]

  def Mod(f: S => S) = Get.flatMap(s => Put(f(s)))

  val handler = DefaultStateHandler[S, this.type](this)
}


object DefaultStateHandler {
  def apply[S, Fx <: State[S]](fx: Fx) = new fx.Unary[S] with fx.Sequential {
    type Result[A] = (S, A)

    def onReturn[A, U](a: A): A !@! U =
      s => Return((s, a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Get => s => k(s)(s)
        case fx.Put(s) => _ => k(())(s)
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => tma(s1).flatMap {
        case (s2, a) => tmb(s2).flatMap {
          case (s3, b) => k((a, b))(s3)
        }
      }
  }
}
