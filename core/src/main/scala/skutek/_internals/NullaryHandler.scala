package skutek
package _internals
import scala.reflect.ClassTag


trait NullaryDriver extends Driver {
  final type Secret[A, -U] = Result[A] !! U
  final def onReveal[A, U](aa: Secret[A, U]) = aa
  final def onConceal[A, B, U, V](eff: A !! U, f: A => Secret[B, V]): Secret[B, U with V] = eff.flatMap(f)
}


abstract class NullaryHandler[Fx](implicit val implicitTag: ClassTag[Fx]) extends BaseHandlerWithSelfDriver with NullaryDriver
