package skutek
import scala.reflect.ClassTag


sealed trait Effectful[+A, -U] {
  final def map[B](f: A => B): B !! U = flatMap[B, U](a => Return(f(a)))
  final def flatMap[B, V](f: A => B !! V): B !! U with V = FlatMapped(this, f)
  final def flatten[B, V](implicit ev: A <:< (B !! V)): B !! U with V = flatMap(ev)
  final def *![B, V](that: B !! V): (A, B) !! U with V = Product(this, that)
  final def *<![B, V](that: B !! V): A !! U with V = *!(that).map(_._1)
  final def *>![B, V](that: B !! V): B !! U with V = *!(that).map(_._2)
  final def upCast[V <: U] = this: A !! V
  final def sideCast[V] = asInstanceOf[A !! V]
}


final case class Return[+A](value: A) extends Effectful[A, Any]

object Return {
  def apply(): Return[Unit] = returnUnit
  private val returnUnit = Return(())
}


private[skutek] final case class FlatMapped[A, +B, -U, -V](eff: A !! U, cont: A => B !! V) extends Effectful[B, U with V]

private[skutek] final case class Product[+A, +B, -U, -V](eff1: A !! U, eff2: B !! V) extends Effectful[(A, B), U with V] 

private[skutek] final case class FilterFail[U]() extends Effectful[Nothing, U]


protected[skutek] sealed trait AnyOperation[+A, -U] extends Effectful[A, U] {
  val tag: Any
  def stripTag: Operation[_, _]
}


abstract class Operation[+A, Fx](implicit implicitTag: ClassTag[Fx]) extends AnyOperation[A, Fx] { outer =>
  val tag = implicitTag
  def stripTag = this

  final def @![Tag](explicitTag: Tag): A !! (Fx @! Tag) = 
    new AnyOperation[A, Fx @! Tag] {
      val tag = explicitTag
      def stripTag = outer
      override def toString = s"$outer @! $tag"
    }
}


protected trait Effectful_exports {

  type !![+A, -U] = Effectful[A, U]

  implicit class Eff_extension[A, U](thiz: A !! U) {
    def downCast[V >: U] = thiz.sideCast[V]
    def withFilter(f: A => Boolean)(implicit ev: U <:< FilterableEffect): A !! U = thiz.flatMap(a => if (f(a)) Return(a) else FilterFail[U])
  }

  implicit class EffOfPair_extension[+A, +B, -U](thiz: (A, B) !! U) {
    def map2[C](f: (A, B) => C): C !! U = thiz.map(f.tupled)
  }
}
