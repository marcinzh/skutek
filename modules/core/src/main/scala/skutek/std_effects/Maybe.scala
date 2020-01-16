package skutek.std_effects
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect


trait Maybe extends Effect.Filterable {
  case object Fail extends Operation[Nothing]

  def from[A](ma: Option[A]): A !! ThisEffect = 
    ma match {
      case Some(a) => Return(a)
      case None => Fail
    }

  val handler = DefaultMaybeHandler(this)
}


object DefaultMaybeHandler {
  def apply[Fx <: Maybe](fx: Fx) = new fx.Nullary with fx.Parallel {
    type Result[A] = Option[A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Some(a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Fail => Return(None)
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap {
        case (Some(a), Some(b)) => k((a, b))
        case _ => Return(None)
      }
      
    val onFail = Some(fx.Fail)
  }
}
