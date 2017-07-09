package skutek
import _internals._

sealed trait Writer[T]
object Writer extends UnaryEffectCompanion[Writer[?]]

sealed trait WriterOperation[A, T] extends Operation[A, Writer[T]]
case class Tell[T](value: T) extends WriterOperation[Unit, T]


abstract class WriterHandler[T] extends UnaryHandler[Writer[T]] {
  type Result[A] = (A, Stan)
  type Op[A] = WriterOperation[A, T]

  def add(c1: Stan, c2: Stan): Stan
  def single(x: T): Stan

  final def onReturn[A](a: A) = c => Return((a, c))

  final def onOperation[A, B, U](op: Op[A], k: A => Stan => Result[B] !! U): Stan => Result[B] !! U = op match {
    case Tell(x) => c => k(())(add(c, single(x)))
  }

  final def onProduct[A, B, C, U](
    x: Stan => Result[A] !! U, 
    y: Stan => Result[B] !! U, 
    k: ((A, B)) => Stan => Result[C] !! U
  ): Stan => Result[C] !! U = 
    (c: Stan) => 
      (x(initial) *! y(initial)).flatMap { 
        case ((a, c1), (b, c2)) => k((a, b))(add(add(c, c1), c2)) 
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
    def add(c1: Stan, c2: Stan) = c1 ++ c2
    def single(x: T) = Vector(x)
  }
}
