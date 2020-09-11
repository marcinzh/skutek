package skutek.utils
import scala.collection.compat._
import skutek.abstraction._


trait Fold_exports {
  implicit class Fold_TraversableOnceOfComputation_extension[A, C[X] <: IterableOnce[X]](thiz: C[A]) {
    def foldLeft_!![U, B](z: B)(op: (B, A) => B !! U): B !! U = foldLeftIt(thiz.iterator, z)(op)

    def reduceLeft_!![U](op: (A, A) => A !! U): A !! U = {
      val it = thiz.iterator
      val z = it.next()
      foldLeftIt(it, z)(op)
    }

    def reduceLeftOption_!![U](op: (A, A) => A !! U): Option[A] !! U = {
      val it = thiz.iterator
      if (it.isEmpty)
        Return(None) 
      else {
        val z = it.next()
        foldLeftIt(it, z)(op).map(Some(_))
      }
    }

    private def foldLeftIt[U, B](it: Iterator[A], z: B)(op: (B, A) => B !! U): B !! U =
      it.foldLeft(Return(z).upCast[U]) {
        case (b_!, a) => for {
          b <- b_!
          b2 <- op(b, a)
        } yield b2
      }
  }


  implicit class Fold_IterableOfComputation_extension[A, C[X] <: Iterable[X]](thiz: C[A]) {
    def foldRight_!![U, B](z: B)(op: (A, B) => B !! U): B !! U =
      thiz.foldRight(Return(z).upCast[U]) {
        case (a, b_!) => for {
          b <- b_!
          b2 <- op(a, b)
        } yield b2
      }

    def reduceRight_!![U](op: (A, A) => A !! U): A !! U =
      thiz.init.foldRight_!!(thiz.last)(op)

    def reduceRightOption_!![U](op: (A, A) => A !! U): Option[A] !! U =
      if (thiz.isEmpty)
        Return(None)
      else
        reduceRight_!!(op).map(Some(_))
  }
}
