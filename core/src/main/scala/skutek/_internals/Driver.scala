package skutek
package _internals


trait DriverAndHandler {
  type Effects
  type Result[A]
}


trait Driver extends DriverAndHandler {
  type Op[A] <: Operation[A, Effects]
  type Secret[A, -U]
  def onReturn[A](a: A): Secret[A, Any]
  def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U]
  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U]
  def onConceal[A, B, U, V](a_! : A !! U, f: A => Secret[B, V]): Secret[B, U with V]
  def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U
  def onFilterFail: Option[Op[Nothing]] = None
}


trait StatelessDriver extends Driver {
  type Secret[A, -U] = Result[A] !! U
  final def onConceal[A, B, U, V](a_! : A !! U, f: A => Secret[B, V]): Secret[B, U with V] = a_!.flatMap(f)
  final def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U = aa
}


trait StatefulDriver extends Driver {
  type Secret[A, -U] = Stan => Result[A] !! U
  type Stan
  def initial: Stan
  final def onConceal[A, B, U, V](a_! : A !! U, f: A => Secret[B, V]): Secret[B, U with V] = s => a_!.flatMap(a => f(a)(s))
  final def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U = aa(initial)
  final override def onFilterFail: Option[Op[Nothing]] = None
}


trait StatefulDriver2 extends StatefulDriver {
  type Result[A] = (A, Stan)
  final def onReturn[A](a: A) = s => Return((a, s))
}
