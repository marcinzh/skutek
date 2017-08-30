package skutek
import _internals._

sealed trait Maybe extends FilterableEffect
object Maybe extends EffectCompanion0[Maybe]

sealed trait Naught extends Operation[Nothing, Maybe]
case object Naught extends Naught

sealed trait MaybeHandler extends StatelessHandler.NoSecret[Maybe]
case object MaybeHandler extends MaybeHandler {
  type Result[A] = Option[A]
  type Op[A] = Naught

  def onReturn[A](a: A) = Return(Some(a))

  def onOperation[A, B, U](op: Op[A], k: A => Result[B] !! U): Result[B] !! U = Return(None)
  
  def onProduct[A, B, C, U](effA: Result[A] !! U, effB: Result[B] !! U, k: ((A, B)) => Result[C] !! U): Result[C] !! U =
    (effA *! effB).flatMap { 
      case (Some(a), Some(b)) => k((a, b))
      case _ => Return(None)
    }

  override val onFilterFail = Some(Naught)
}


trait Maybe_exports {
  implicit class OptionToEffectful[T](thiz: Option[T]) {
    def toEff: T !! Maybe = thiz match {
      case Some(x) => Return(x)
      case _ => Naught
    }
  }
}
