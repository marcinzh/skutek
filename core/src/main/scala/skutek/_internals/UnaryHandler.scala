package skutek
package _internals
import scala.reflect.ClassTag


trait UnaryDriver extends Driver {
  type Stan
  def initial: Stan

  final type Secret[A, -U] = Stan => Result[A] !! U
  final def onReveal[A, U](aa: Secret[A, U]) = aa(initial)
  final def onConceal[A, B, U, V](eff: A !! U, f: A => Secret[B, V]): Secret[B, U with V] = c => eff.flatMap(a => f(a)(c))
}


abstract class UnaryHandler[Fx](implicit val implicitTag: ClassTag[Fx]) extends BaseHandlerWithSelfDriver with UnaryDriver
