package skutek
package _internals
import scala.reflect.ClassTag


trait SimpleStatefulDriver extends BaseDriver {
  type Stan
  def initial: Stan
  def onOperation[A](op: Op[A], s: Stan): (A, Stan)
}


abstract class SimpleStatefulHandler[Fx](implicit val implicitTag: ClassTag[Fx]) extends BaseHandlerWithDriver with SimpleStatefulDriver { outer =>

  type Result[A] = (A, Stan)

  val driver = new UnaryDriver {
    type Effects = outer.Effects
    type Result[A] = outer.Result[A]
    type Op[A] = outer.Op[A]
    type Stan = outer.Stan

    def initial: Stan = outer.initial
    def onReturn[A](a: A) = s => Return((a, s))

    def onOperation[A, B, U](op: Op[A], k: A => Stan => Result[B] !! U): Stan => Result[B] !! U = 
      s => {
        val (a, s2) = outer.onOperation(op, s)
        k(a)(s2)
      }

    def onProduct[A, B, C, U](
      x: Stan => Result[A] !! U, 
      y: Stan => Result[B] !! U, 
      k: ((A, B)) => Stan => Result[C] !! U
    ): Stan => Result[C] !! U = 
      (s: Stan) => {
        x(s).flatMap { case (a, s2) =>
          y(s2).flatMap { case (b, s3) =>
            k((a, b))(s3)
          }
        }
      }
  }

  def exec = new MappedHandler[Lambda[A => Stan]] {
    def apply[A](pair: (A, Stan)) = pair._2
  }

  def eval = new MappedHandler[Lambda[A => A]] {
    def apply[A](pair: (A, Stan)) = pair._1
  }
}
