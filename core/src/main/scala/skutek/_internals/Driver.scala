package skutek
package _internals


trait BaseDriver {
  type Effects
  type Result[X]
  type Op[A] <: Operation[A, Effects]
}


trait Driver extends BaseDriver {
  type Secret[A, -U]

  def onReturn[A](a: A): Secret[A, Any]
  def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U]
  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U]
  def onConceal[A, B, U, V](eff: A !! U, f: A => Secret[B, V]): Secret[B, U with V]
  def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U
  def onFilterFail: Option[Op[Nothing]]
}
