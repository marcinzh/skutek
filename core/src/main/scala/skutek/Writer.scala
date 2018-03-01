package skutek
import _internals._

sealed trait Writer[T]
object Writer extends EffectCompanion1[Writer]

sealed trait WriterOperation[A, T] extends Operation[A, Writer[T]]
case class Tell[T](value: T) extends WriterOperation[Unit, T]


abstract class WriterHandler[T] extends StatefulHandler2[Writer[T]] {
  type Op[A] = WriterOperation[A, T]

  def add(s1: Stan, s2: Stan): Stan
  def single(x: T): Stan

  def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U] = op match {
    case Tell(x) => s => k(())(add(s, single(x)))
  }

  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U] =
    (s1: Stan) => 
      (aa(initial) *! bb(initial)).flatMap { 
        case ((a, s2), (b, s3)) => k((a, b))(add(add(s1, s2), s3)) 
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
