package skutek.abstraction


trait Handler { outer =>
  type Effects
  type Result[A]

  def interpret[A, U](ma: A !! U with Effects): Result[A] !! U

  final def handle[U] = new HandleApply[U]
  final class HandleApply[U] {
    def apply[A, V](ma: A !! V)(implicit ev: HandlerConforms[U, V, Effects]) = interpret[A, U](ev(ma))
  }

  final def run[A](eff: A !! Effects): Result[A] = handle[Any](eff).run

  final def <<<![H <: Handler](that: H) = new Composed(that)
  final def >>>![H <: Handler](that: H) = that <<<! this

  class Composed[H <: Handler](val h: H) extends Handler {
    final override type Effects = outer.Effects with h.Effects
    final override type Result[A] = outer.Result[h.Result[A]]

    final override def interpret[A, U](ma: A !! U with Effects): Result[A] !! U =
      outer.interpret[h.Result[A], U](
        h.interpret[A, U with outer.Effects](ma)
      )
  }

  abstract class Into[F[_]] extends Handler {
    final override type Result[A] = F[A]
    final override type Effects = outer.Effects

    final override def interpret[A, U](ma: A !! U with Effects): Result[A] !! U =
      outer.interpret[A, U](ma).map(apply)
    
    def apply[A](x: outer.Result[A]): F[A]
  }
}


object Handler {
  type Apply[F[_], U] = Handler {
    type Effects = U
    type Result[A] = F[A]
  }
}


trait Handler_exports {
  type >>>![H1 <: Handler, H2 <: Handler] = H2 <<<! H1

  type <<<![H1 <: Handler, H2 <: Handler] = Handler {
    type Effects = H1#Effects with H2#Effects
    type Result[A] = H1#Result[H2#Result[A]]
  }

  implicit class ComputationHandleWith_extension[A, U](thiz: A !! U) {
    def handleWith[V] = new HandleWithApply[V]
    final class HandleWithApply[V] {
      def apply[H <: Handler](h: H)(implicit ev: HandlerConforms[V, U, h.Effects]) = h.handle[V](thiz)
    }

    def runWith(h: Handler { type Effects <: U }) = handleWith[Any](h).run
  }
}
