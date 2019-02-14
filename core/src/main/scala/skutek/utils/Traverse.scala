package skutek.utils
import skutek.abstraction._
import scala.collection.generic.CanBuildFrom
// import scala.collection.mutable.Builder


trait Traverse_exports {
  implicit class IterableOfComputation_extension[+A, -U, S[+X] <: Iterable[X]](thiz: S[A !! U]) {
    def traverseVoid: Unit !! U = thiz.foldLeft(Return.widen[U])(_ *<! _)

    def traverseVoidShort: Unit !! U = {
      def loop(todos: Iterable[A !! U]): Unit !! U =
        if (todos.isEmpty)
          Return
        else 
          todos.head.flatMap(_ => loop(todos.tail))

      loop(thiz)
    }
  }


  implicit class IterableOfComputationCBF_extension[+A, -U, S[+X] <: Iterable[X]](thiz: S[A !! U])(implicit cbf: CanBuildFrom[S[A !! U], A, S[A]]) {
    def traverse: S[A] !! U =
      thiz.foldLeft(Return(Vector.empty[A]).widen[U]) { 
        case (as_!, a_!) => (as_! *! a_!).map2(_ :+ _) 
      }
      .map(as => (cbf() ++= as).result())

    def traverseShort: S[A] !! U = {
      def loop(todos: Iterable[A !! U], accum: Vector[A]): Vector[A] !! U =
        if (todos.isEmpty)
          Return(accum)
        else 
          todos.head.flatMap(a => loop(todos.tail, accum :+ a))

      loop(thiz, Vector())
      .map(as => (cbf() ++= as).result())
    }
  }


  implicit class OptionOfComputation_extension[+A, -U](thiz: Option[A !! U]) {
    def traverse: Option[A] !! U =
      thiz match {
        case Some(ma) => ma.map(Some(_))
        case None => Return(None)
      }
    def traverseVoid: Unit !! U =
      thiz match {
        case Some(ma) => ma.void
        case None => Return
      }
    def traverseShort = traverse
    def traverseVoidShort = traverseVoid
  }


  implicit class EitherOfComputation_extension[+A, +T, -U](thiz: Either[T, A !! U]) {
    def traverse: Either[T, A] !! U =
      thiz match {
        case Right(ma) => ma.map(Right(_))
        case Left(x) => Return(Left(x))
      }
    def traverseVoid: Unit !! U =
      thiz match {
        case Right(ma) => ma.void
        case Left(_) => Return
      }
    def traverseShort = traverse
    def traverseVoidShort = traverseVoid
  }
}