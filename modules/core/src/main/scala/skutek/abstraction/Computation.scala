package skutek.abstraction
import Computation._


sealed trait Computation[+A, -U] {
  final def map[B](f: A => B): B !! U = flatMap[B, U](a => Return(f(a)))
  final def flatMap[B, V](f: A => B !! V): B !! U with V = FlatMap(this, f)
  final def flatten[B, V](implicit ev: A <:< (B !! V)): B !! U with V = flatMap(ev)

  final def *![B, V](that: B !! V): (A, B) !! U with V = Product(this, that)
  final def *<![B, V](that: B !! V): A !! U with V = *!(that).map(_._1)
  final def *>![B, V](that: B !! V): B !! U with V = *!(that).map(_._2)

  final def **![B, V](that : => B !! V): (A, B) !! U with V = for { a <- this; b <- that } yield (a, b)
  final def **<![B, V](that : => B !! V): A !! U with V = for { a <- this; b <- that } yield a
  final def **>![B, V](that : => B !! V): B !! U with V = for { a <- this; b <- that } yield b

  final def void: Unit !! U = map(_ => ())
  final def widen[V <: U] = this: A !! V
  final def forceFilterable = this: A !! U with FilterableEffect

  final def run(implicit ev: Any <:< U): A = pureLoop
  
  @annotation.tailrec final private[skutek] def pureLoop: A = this match {
    case Return(a) => a
    case FlatMap(ma, k) => ma match {
      case Return(a) => k(a).pureLoop
      case FlatMap(ma, j) => ma.flatMap(a => j(a).flatMap(k)).pureLoop
      case Product(ma, mb) => ma.flatMap(a => mb.flatMap(b => k((a, b)))).pureLoop
      case _ => sys.error(s"Unhandled effect: $ma")
    }
    case _ => map(a => a).pureLoop
  }
}

case class Return[+A](value: A) extends Computation[A, Any]

object Return extends Return(()) {
  def apply[U] : Unit !! U = this.widen[U]
}


object Computation {
  case class FlatMap[A, +B, -U](ma: A !! U, k: A => B !! U) extends Computation[B, U]
  case class Product[+A, +B, -U](ma: A !! U, mb: B !! U) extends Computation[(A, B), U]
  case object FilterFail extends Computation[Nothing, Any]

  trait Operation[+A, -U] extends Computation[A, U] {
    def thisEffect: Effect
  }
}


trait Computation_exports {
  type !![+A, -U] = Computation[A, U]

  implicit class Computation_extension[A, U](thiz: A !! U) {
    def withFilter(f: A => Boolean)(implicit ev: U <:< FilterableEffect): A !! U = 
      thiz.flatMap(a => if (f(a)) Return(a) else FilterFail)
  }

  implicit class ComputationOfPair_extension[+A, +B, -U](thiz: (A, B) !! U) {
    def map2[C](f: (A, B) => C): C !! U = thiz.map(f.tupled)
    def flatMap2[C, V](f: (A, B) => C !! V): C !! U with V = thiz.flatMap(f.tupled)
  }

  def Trampoline[A, U](ma : => A !! U): A !! U = Return.flatMap(_ => ma)
  def Eval[A](a : => A): A !! Any = Return.flatMap(_ => Return(a))
}
