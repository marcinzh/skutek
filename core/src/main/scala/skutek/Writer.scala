package skutek
import _internals._

sealed trait Writer[T]
object Writer extends EffectCompanion1[Writer]

sealed trait WriterOperation[A, T] extends Operation[A, Writer[T]]
case class Tell[T](value: T) extends WriterOperation[Unit, T]


abstract class WriterHandler[T] extends StatefulHandler.NoSecret[Writer[T]] {
  type Result[A] = (A, Stan)
  type Op[A] = WriterOperation[A, T]

  def add(s1: Stan, s2: Stan): Stan
  def single(x: T): Stan

  final def onReturn[A](a: A) = s => Return((a, s))

  final def onOperation[A, B, U](op: Op[A], k: A => Stan => Result[B] !! U): Stan => Result[B] !! U = op match {
    case Tell(x) => s => k(())(add(s, single(x)))
  }

  final def onProduct[A, B, C, U](
    x: Stan => Result[A] !! U, 
    y: Stan => Result[B] !! U, 
    k: ((A, B)) => Stan => Result[C] !! U
  ): Stan => Result[C] !! U = 
    (s: Stan) => 
      (x(initial) *! y(initial)).flatMap { 
        case ((a, s1), (b, s2)) => k((a, b))(add(add(s, s1), s2)) 
      }

  def exec = new MappedHandler[Lambda[A => Stan]] {
    def apply[A](pair: (A, Stan)) = pair._2
  }

  def eval = new MappedHandler[Lambda[A => A]] {
    def apply[A](pair: (A, Stan)) = pair._1
  }
}


object WriterHandler {

  def strings = seq[String]

  def seq[T]() = new WriterHandler[T] {
    type Stan = Vector[T]
    def initial = Vector.empty[T]
    def add(s1: Stan, s2: Stan) = s1 ++ s2
    def single(x: T) = Vector(x)
  }

  def monoid[T](zero: T, op: (T, T) => T) = new WriterHandler[T] {
    type Stan = T
    def initial = zero
    def add(x: T, y: T) = op(x, y)
    def single(x: T) = x
  }
}
