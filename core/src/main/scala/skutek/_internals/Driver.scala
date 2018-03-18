package skutek
package _internals


trait DriverAndHandler {
  type Effects
  type Result[A]
}


trait Driver extends DriverAndHandler {
  type Op[A] <: Operation[A, Effects]
  type Secret[A, -U]
  type Cont[A, B, U] = (A => Secret[B, U]) => Secret[B, U]
  def onReturn[A](a: A): Secret[A, Any]
  def onOperation[A, B, U](op: Op[A]): Cont[A, B, U]
  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U]): Cont[(A, B), C, U]
  def onConceal[A, B, U](a_! : A !! U): Cont[A, B, U]
  def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U
  def onFilterFail: Option[Op[Nothing]] = None
}


trait StatelessDriver extends Driver {
  type Secret[A, -U] = Result[A] !! U
  def onConceal[A, B, U](a_! : A !! U): Cont[A, B, U] = k => a_!.flatMap(k)
  def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U = aa
}


trait StatefulDriver extends Driver {
  type Secret[A, -U] = Stan => Result[A] !! U
  type Stan
  def initial: Stan
  def onConceal[A, B, U](a_! : A !! U): Cont[A, B, U] = k => s => a_!.flatMap(a => k(a)(s))
  def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U = aa(initial)
  final override def onFilterFail: Option[Op[Nothing]] = None
}


trait StatefulDriver2 extends StatefulDriver {
  type Result[A] = (A, Stan)
  final def onReturn[A](a: A) = s => Return((a, s))
}
