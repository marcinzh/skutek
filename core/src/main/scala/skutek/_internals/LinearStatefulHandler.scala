package skutek
package _internals
import scala.reflect.ClassTag


trait LinearStatefulDriver extends BaseDriver { outer =>
  type Stan

  def initial: Stan
  def onOperation[A](op: Op[A], s: Stan): (A, Stan)
  def onReveal[A](a: A, s: Stan): Result[A]


  final def toUniversalDriver = new StatefulDriver {
    type Effects = outer.Effects
    type Result[A] = outer.Result[A]
    type Op[A] = outer.Op[A]
    type Secret[A] = (A, Stan)
    type Stan = outer.Stan

    def initial: Stan = outer.initial
    
    def onReturn[A](a: A) = s => Return((a, s))

    def onOperation[A, B, U](op: Op[A], k: A => Stan => Secret[B] !! U): Stan => Secret[B] !! U = 
      (s: Stan) => {
        val (a, s2) = outer.onOperation(op, s)
        k(a)(s2)
      }

    def onProduct[A, B, C, U](aa: Stan => Secret[A] !! U, bb: Stan => Secret[B] !! U, k: ((A, B)) => Stan => Secret[C] !! U): Stan => Secret[C] !! U =
      (s: Stan) => aa(s).flatMap { 
        case (a, s2) => bb(s2).flatMap { 
          case (b, s3) => k((a, b))(s3)
        }
      }

    def onReveal[A, U](aa: Stan => Secret[A] !! U): Stan => Result[A] !! U =
      (s: Stan) => aa(s).map {
        case (a, s2) => outer.onReveal(a, s2)
      }

  }.toUniversalDriver
}


object LinearStatefulDriver {
  trait Simple extends LinearStatefulDriver {
    type Result[A] = (A, Stan)
    final def onReveal[A](a: A, s: Stan): (A, Stan) = (a, s)
  }
}


abstract class LinearStatefulHandler[Fx](implicit tag: ClassTag[Fx]) extends BaseHandlerWithDriver with LinearStatefulDriver {
  val driver = toUniversalDriver
}


object LinearStatefulHandler {
  abstract class Simple[Fx](implicit tag: ClassTag[Fx]) extends LinearStatefulHandler with LinearStatefulDriver.Simple {
    def exec = new MappedHandler[Lambda[A => Stan]] {
      def apply[A](pair: (A, Stan)) = pair._2
    }

    def eval = new MappedHandler[Lambda[A => A]] {
      def apply[A](pair: (A, Stan)) = pair._1
    }
  }
}
