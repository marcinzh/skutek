package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait Maybe extends FilterableEffectImpl {
  case object Fail extends Operation[Nothing]

  def from[A](ma: Option[A]): A !! ThisEffect = 
    ma match {
      case Some(a) => Return(a)
      case None => Fail
    }

  def handler = new Nullary with Parallel {
    type Result[A] = Option[A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Some(a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Fail => Return(None)
      }

    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (Some(a), Some(b)) => k((a, b))
        case _ => Return(None)
      }
      
    val onFail = Some(Fail)
  }
}
