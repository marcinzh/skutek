package skutek
import scala.collection.generic.CanBuildFrom
// import scala.collection.mutable.Builder


protected trait Traverse_exports {

  implicit class IterableOfEffs_extension[+A, -U, S[+X] <: Iterable[X]](thiz: S[A !! U]) {

    def traverseVoid: Unit !! U = thiz.foldLeft(Return().upCast[U])(_ *<! _)

    def traverseLazyVoid: Unit !! U = {
      def loop(todos: Iterable[A !! U]): Unit !! U =
        if (todos.isEmpty)
          Return()
        else 
          todos.head.flatMap(_ => loop(todos.tail))

      loop(thiz)
    }
  }


  implicit class IterableOfEffsCBF_extension[+A, -U, S[+X] <: Iterable[X]](thiz: S[A !! U])(implicit cbf: CanBuildFrom[S[A !! U], A, S[A]]) {

    def traverse: S[A] !! U = {
      thiz.foldLeft(Return(Vector.empty[A]).upCast[U]) { case (as_!, a_!) => (as_! *! a_!).map2(_ :+ _) }
      .map(as => (cbf() ++= as).result())
    }

    def traverseLazy: S[A] !! U = {
      def loop(todos: Iterable[A !! U], accum: Vector[A]): Vector[A] !! U =
        if (todos.isEmpty)
          Return(accum)
        else 
          todos.head.flatMap(a => loop(todos.tail, accum :+ a))

      loop(thiz, Vector())
      .map(as => (cbf() ++= as).result())
    }
  }


  implicit class OptionOfEff_extension[+A, -U](thiz: Option[A !! U]) {
    def traverse: Option[A] !! U = thiz match {
      case Some(eff) => eff.map(Some(_))
      case None => Return(None)
    }
    def traverseVoid = traverse.map(_.map(_ => ()))
    def traverseLazy = traverse
    def traverseLazyVoid = traverseVoid
  }


  implicit class EitherOfEff_extension[+A, +T, -U](thiz: Either[T, A !! U]) {
    def traverse: Either[T, A] !! U = thiz match {
      case Right(eff) => eff.map(Right(_))
      case Left(x) => Return(Left(x))
    }
    def traverseVoid = traverse.map(_.right.map(_ => ()))
    def traverseLazy = traverse
    def traverseLazyVoid = traverseVoid
  }
}
