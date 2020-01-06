package skutek.utils
import skutek.abstraction._


trait Accumulator[E, S] {
  def zero: S
  def one(e: E): S
  def add(s1: S, s2: S): S
}

object Accumulator {
  def intoSeq[A] : Accumulator[A, Vector[A]] =
    new Accumulator[A, Vector[A]] {
      def zero = Vector()
      def one(a: A) = Vector(a)
      def add(as: Vector[A], bs: Vector[A]) = as ++ bs
    }

  def intoSet[A] : Accumulator[A, Set[A]] =
    new Accumulator[A, Set[A]] {
      def zero = Set()
      def one(a: A) = Set(a)
      def add(as: Set[A], bs: Set[A]) = as | bs
    }

  def intoMap[K, V] : Accumulator[(K, V), Map[K, V]] =
    new Accumulator[(K, V), Map[K, V]] {
      def zero = Map()
      def one(kv: (K, V)) = Map(kv)
      def add(m1: Map[K, V], m2: Map[K, V]) = {
        val m = m1 ++ m2
        if (m.size == m1.size + m2.size)
          m
        else {
          val bads = m1.keySet & m2.keySet
          val bads2 = bads.toSeq.take(10).map(_.toString.take(80).sorted)
          sys.error(s"Found ${bads.size} key collisions in intoMap:\n${bads2.mkString("  ", "\n  ", "")}")
        }
      }
    }

  def fold[A](z: A)(op: (A, A) => A): Accumulator[A, A] =
    new Accumulator[A, A] {
      def zero = z
      def one(a: A) = a
      def add(a: A, b: A) = op(a, b)
    }

  def reduceOption[A](op: (A, A) => A): Accumulator[A, Option[A]] =
    new Accumulator[A, Option[A]] {
      def zero = None
      def one(a: A) = Some(a)
      def add(ma: Option[A], mb: Option[A]) = ma.flatMap(a => mb.map(b => op(a, b)))
    }

  def firstOption[A] : Accumulator[A, Option[A]] =
    new Accumulator[A, Option[A]] {
      def zero = None
      def one(a: A) = Some(a)
      def add(ma: Option[A], mb: Option[A]) = ma orElse mb
    }

  def lastOption[A] : Accumulator[A, Option[A]] =
    new Accumulator[A, Option[A]] {
      def zero = None
      def one(a: A) = Some(a)
      def add(ma: Option[A], mb: Option[A]) = mb orElse ma
    }
}
