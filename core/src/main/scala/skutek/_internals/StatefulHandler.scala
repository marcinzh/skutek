package skutek
package _internals
import scala.reflect.ClassTag


trait StatefulDriver extends BaseDriver { outer =>
  type Secret[A]
  type Stan

  def initial: Stan
  def onReturn[A](a: A): Stan => Secret[A] !! Any
  def onOperation[A, B, U](op: Op[A], k: A => Stan => Secret[B] !! U): Stan => Secret[B] !! U
  def onProduct[A, B, C, U](aa: Stan => Secret[A] !! U, bb: Stan => Secret[B] !! U, k: ((A, B)) => Stan => Secret[C] !! U): Stan => Secret[C] !! U
  def onReveal[A, U](aa: Stan => Secret[A] !! U): Stan => Result[A] !! U


  final def toUniversalDriver = new Driver {
    type Effects = outer.Effects
    type Result[A] = outer.Result[A]
    type Op[A] = outer.Op[A]
    type Secret[A, -U] = Stan => outer.Secret[A] !! U

    def onReturn[A](a: A): Secret[A, Any] = outer.onReturn(a)
    def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U] = outer.onOperation(op, k)
    def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U] = outer.onProduct(aa, bb, k)
    def onFilterFail: Option[Op[Nothing]] = None

    def onConceal[A, B, U, V](eff: A !! U, f: A => Secret[B, V]): Secret[B, U with V] = s => eff.flatMap(a => f(a)(s))
    def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U = outer.onReveal(aa)(initial)
  }
}


object StatefulDriver {
  trait NoSecret extends StatefulDriver {
    final type Secret[A] = Result[A]
    final def onReveal[A, U](aa: Stan => Secret[A] !! U): Stan => Result[A] !! U = aa
  }
}


abstract class StatefulHandler[Fx](implicit tag: ClassTag[Fx]) extends BaseHandlerWithDriver with StatefulDriver {
  val driver = toUniversalDriver
}


object StatefulHandler {
  abstract class NoSecret[Fx](implicit tag: ClassTag[Fx]) extends StatefulHandler with StatefulDriver.NoSecret
}
