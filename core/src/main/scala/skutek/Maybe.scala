package skutek
import _internals._

sealed trait Maybe extends FilterableEffect
object Maybe extends EffectCompanion0[Maybe]

sealed trait Naught extends Operation[Nothing, Maybe]
case object Naught extends Naught

sealed trait MaybeHandler extends StatelessHandler[Maybe]
case object MaybeHandler extends MaybeHandler {
  type Result[A] = Option[A]
  type Op[A] = Naught

  def onReturn[A](a: A): Secret[A, Any] = 
    Return(Some(a))

  def onOperation[A, B, U](op: Op[A]): Cont[A, B, U] = 
    _ => Return(None)

  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U]): Cont[(A, B), C, U] = 
    k => (aa *! bb).flatMap { 
      case (Some(a), Some(b)) => k((a, b))
      case _ => Return(None)
    }

  override val onFilterFail = Some(Naught)
}


trait Maybe_exports {
  implicit class OptionToComputation[T](thiz: Option[T]) {
    def toEff: T !! Maybe = thiz match {
      case Some(x) => Return(x)
      case _ => Naught
    }

    def toEff[Tag](tag: Tag): T !! (Maybe @! Tag) = thiz match {
      case Some(x) => Return(x)
      case _ => Naught @! tag
    }
  }
}
