package skutek
package _internals
import scala.reflect.ClassTag


trait StatelessDriver extends BaseDriver { outer =>
  type Secret[A]

  def onReturn[A](a: A): Secret[A] !! Any
  def onOperation[A, B, U](op: Op[A], k: A => Secret[B] !! U): Secret[B] !! U
  def onProduct[A, B, C, U](aa: Secret[A] !! U, bb: Secret[B] !! U, k: ((A, B)) => Secret[C] !! U): Secret[C] !! U
  def onReveal[A, U](aa: Secret[A] !! U): Result[A] !! U
  def onFilterFail: Option[Op[Nothing]] = None


  final def toUniversalDriver = new Driver {
    type Effects = outer.Effects
    type Result[A] = outer.Result[A]
    type Op[A] = outer.Op[A]
    type Secret[A, -U] = outer.Secret[A] !! U

    def onReturn[A](a: A): Secret[A, Any] = outer.onReturn(a)
    def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U] = outer.onOperation(op, k)
    def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U] = outer.onProduct(aa, bb, k)
    def onReveal[A, U](aa: Secret[A, U]) = outer.onReveal(aa)
    def onFilterFail: Option[Op[Nothing]] = outer.onFilterFail

    def onConceal[A, B, U, V](eff: A !! U, f: A => Secret[B, V]): Secret[B, U with V] = eff.flatMap(f)
  }
}


object StatelessDriver {
  trait NoSecret extends StatelessDriver {
    final type Secret[A] = Result[A]
    final def onReveal[A, U](aa: Secret[A] !! U): Result[A] !! U = aa
  }
}


abstract class StatelessHandler[Fx](implicit tag: ClassTag[Fx]) extends BaseHandlerWithDriver with StatelessDriver {
  val driver = toUniversalDriver
}


object StatelessHandler {
  abstract class NoSecret[Fx](implicit tag: ClassTag[Fx]) extends StatelessHandler with StatelessDriver.NoSecret
}
