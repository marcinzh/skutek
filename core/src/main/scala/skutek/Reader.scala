package skutek
import _internals._

sealed trait Reader[E]
object Reader extends UnaryEffectCompanion[Reader[?]]

sealed trait ReaderOperation[A, E] extends Operation[A, Reader[E]]
case class Ask[E]() extends ReaderOperation[E, E]

case class ReaderHandler[E](env: E) extends SimpleStatelessHandler[Reader[E]] {
  type Op[A] = ReaderOperation[A, E]

  def onOperation[A](op: Op[A]): A = op match {
    case _: Ask[_] => env
  }
}
