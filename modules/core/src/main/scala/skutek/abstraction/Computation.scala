package skutek.abstraction
import skutek.abstraction.internals.aux.{CanRunPure, CanRunImpure, CanHandle}
import skutek.abstraction.internals.interpreter.Interpreter
import ComputationCases._


sealed trait Computation[+A, -U] {
  final def map[B](f: A => B): B !! U = flatMap[B, U](a => Return(f(a)))
  final def flatMap[B, V](f: A => B !! V): B !! U with V = FlatMap(this, f)
  final def flatten[B, V](implicit ev: A <:< (B !! V)): B !! U with V = flatMap(ev)

  final def *![B, V](that: B !! V): (A, B) !! U with V = Product(this, that)
  final def *<![B, V](that: B !! V): A !! U with V = *!(that).map(_._1)
  final def *>![B, V](that: B !! V): B !! U with V = *!(that).map(_._2)

  final def **![B, V](that : => B !! V): (A, B) !! U with V = flatMap(a => that.map((a, _)))
  final def **<![B, V](that : => B !! V): A !! U with V = flatMap(a => that.map(_ => a))
  final def **>![B, V](that : => B !! V): B !! U with V = flatMap(_ => that)

  final def &![B, V](that: B !! V): B !! U with V = this *>! that
  final def &&![B, V](that : => B !! V): B !! U with V = this **>! that

  final def void: Unit !! U = map(_ => ())
  final def widen[V <: U] = this: A !! V
  final def forceFilterable = this: A !! U with FilterableEffect
}

object Computation {
  def pure(): Unit !! Any = Return
  def pure[A](a: A): A !! Any = Return(a) //@#@TODO bad type
  //@#@TODO:
  // def fail: Nothing !! FailEffect = ???
  // def defer[A, U](ua: => A !! U): A !! U = ???
}

case class Return[+A](value: A) extends Computation[A, Any]

object Return extends Return(()) {
  def apply[U] : Unit !! U = this.widen[U]
}

private[abstraction] object ComputationCases {
  case class FlatMap[A, +B, -U](ma: A !! U, k: A => B !! U) extends Computation[B, U]
  case class Product[+A, +B, -U](ma: A !! U, mb: B !! U) extends Computation[(A, B), U]
  case object FilterFail extends Computation[Nothing, Any] //@#@TODO bad type

  trait Operation[+A, -U] extends Computation[A, U] {
    def thisEffect: Effect
  }
}


trait Computation_exports {
  type !![+A, -U] = Computation[A, U]
  def !! = Computation

  implicit class Computation_extension[A, U](thiz: A !! U) {
    def run(implicit ev: CanRunPure[U]): A = Interpreter.runPure(ev(thiz))

    def runWith[H <: Handler](h: H)(implicit ev: CanRunImpure[U, h.Effects]): h.Result[A] =
      h.interpret[A, Any](ev(thiz)).run

    def handleWith[V] : HandleWithApply[V] = new HandleWithApply[V]
    class HandleWithApply[V] {
      def apply[H <: Handler](h: H)(implicit ev: CanHandle[V, U, h.Effects]): h.Result[A] !! V =
        h.interpret[A, V](ev(thiz))
    }

    def withFilter(f: A => Boolean)(implicit ev: U <:< FilterableEffect): A !! U = 
      thiz.flatMap(a => if (f(a)) Return(a) else FilterFail)

    def downCast[V >: U] = thiz.asInstanceOf[Computation[A, V]]
  }

  implicit class ComputationOfPair_extension[+A, +B, -U](thiz: (A, B) !! U) {
    def map2[C](f: (A, B) => C): C !! U = thiz.map(f.tupled)
    def flatMap2[C, V](f: (A, B) => C !! V): C !! U with V = thiz.flatMap(f.tupled)
  }

  def Trampoline[A, U](ma : => A !! U): A !! U = Return.flatMap(_ => ma)
  def Eval[A](a : => A): A !! Any = Return.flatMap(_ => Return(a))

}
