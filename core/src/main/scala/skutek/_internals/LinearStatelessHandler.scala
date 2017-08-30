package skutek
package _internals
import scala.reflect.ClassTag


trait LinearStatelessDriver extends BaseDriver { outer =>

  def onOperation[A](op: Op[A]): A
  def onReveal[A](a: A): Result[A]


  final def toUniversalDriver = new StatelessDriver {
    type Effects = outer.Effects
    type Result[A] = outer.Result[A]
    type Op[A] = outer.Op[A]
    type Secret[A] = A

    def onReturn[A](a: A): Secret[A] !! Any = Return(a)

    def onOperation[A, B, U](op: Op[A], k: A => Secret[B] !! U): Secret[B] !! U = 
      k(outer.onOperation(op))
    
    def onProduct[A, B, C, U](aa: Secret[A] !! U, bb: Secret[B] !! U, k: ((A, B)) => Secret[C] !! U): Secret[C] !! U = 
      (aa *! bb).flatMap(k)

    def onReveal[A, U](aa: Secret[A] !! U): Result[A] !! U = 
      aa.map(outer.onReveal(_))

  }.toUniversalDriver
}


object LinearStatelessDriver {
  trait Simple extends LinearStatelessDriver {
    final type Result[A] = A
    final def onReveal[A](a: A): A = a
  }
}


abstract class LinearStatelessHandler[Fx](implicit tag: ClassTag[Fx]) extends BaseHandlerWithDriver with LinearStatelessDriver {
  val driver = toUniversalDriver
}


object LinearStatelessHandler {
  abstract class Simple[Fx](implicit tag: ClassTag[Fx]) extends LinearStatelessHandler with LinearStatelessDriver.Simple 
}
