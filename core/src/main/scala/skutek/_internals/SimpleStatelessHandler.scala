package skutek
package _internals
import scala.reflect.ClassTag


trait SimpleStatelessDriver extends BaseDriver {
  def onOperation[A](op: Op[A]): A
}


abstract class SimpleStatelessHandler[Fx](implicit val implicitTag: ClassTag[Fx]) extends BaseHandlerWithDriver with SimpleStatelessDriver { outer =>

  type Result[A] = A

  val driver = new NullaryDriver {
    type Effects = outer.Effects
    type Result[A] = outer.Result[A]
    type Op[A] = outer.Op[A]

    def onReturn[A](a: A) = Return(a)

    def onOperation[A, B, U](op: Op[A], k: A => Result[B] !! U): Result[B] !! U = 
      k(outer.onOperation(op))
    
    def onProduct[A, B, C, U](a_! : Result[A] !! U, b_! : Result[B] !! U, k: ((A, B)) => Result[C] !! U): Result[C] !! U =
      (a_! *! b_!).flatMap(k)
  }
}
