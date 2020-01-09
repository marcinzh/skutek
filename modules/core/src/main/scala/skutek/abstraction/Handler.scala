package skutek.abstraction
import mwords.~>
import skutek.abstraction.internals.aux.CanHandle
import skutek.abstraction.internals.handler.PrimitiveHandlerImpl


private[abstraction] trait HandlerStub {
  type Effects
  type Result[A]
}


sealed trait Handler extends HandlerStub {
  final type This = Handler.Apply[Result, Effects]
  final type Into[F[_]] = Result ~> F

  final def handle[U] = new HandleApply[U]
  final class HandleApply[U] {
    def apply[A, V](ma: A !! V)(implicit ev: CanHandle[U, V, Effects]) = doHandle[A, U](ev(ma))
  }

  final def run[A](eff: A !! Effects): Result[A] = handle[Any](eff).run

  final def <<<![H <: Handler](that: H) = HandlerCases.Composed[This, H](this, that)
  final def >>>![H <: Handler](that: H) = that <<<! this

  final def map[F[_]](f: Result ~> F): Handler.Apply[F, Effects] = HandlerCases.Mapped[This, F](this)(f)

  protected[abstraction] def doHandle[A, U](ma: A !! U with Effects): Result[A] !! U
}


object Handler {
  type Apply[F[_], U] = Handler {
    type Effects = U
    type Result[A] = F[A]
  }
}


object HandlerCases {
  final case class Composed[HL <: Handler, HR <: Handler](val lhs: HL, val rhs: HR) extends Handler {
    override type Effects = lhs.Effects with rhs.Effects
    override type Result[A] = lhs.Result[rhs.Result[A]]

    protected[abstraction] override def doHandle[A, U](eff: A !! U with lhs.Effects with rhs.Effects): Result[A] !! U =
      lhs.doHandle[rhs.Result[A], U](
        rhs.doHandle[A, U with lhs.Effects](eff)
      )
  }

  final case class Mapped[H <: Handler, F[_]](that: H)(fun: H#Result ~> F) extends Handler {
    override type Result[A] = F[A]
    override type Effects = that.Effects

    protected[abstraction] override def doHandle[A, U](eff: A !! U with Effects): Result[A] !! U =
      that.doHandle[A, U](eff).map(fun(_))
  }

  trait Nullary extends Handler with PrimitiveHandlerImpl.Nullary {
    final override def doHandle[A, U](ma: A !! U with Effects): Result[A] !! U = interpreter[U](ma)
  }

  trait Unary[S] extends PrimitiveHandlerImpl.Unary[S] { outer =>
    def apply(initial: S): ThisSaturatedHandler = new ThisSaturatedHandler(initial)

    class ThisSaturatedHandler(initial: S) extends Handler {
      final override type Effects = outer.Effects
      final override type Result[A] = outer.Result[A]
      final override def doHandle[A, U](ma: A !! U with Effects): Result[A] !! U = interpreter[U](ma)(initial)
    }
  }

  trait Foreign extends Handler with PrimitiveHandlerImpl.Foreign {
    final override def doHandle[A, U](ma: A !! U with Effects): Result[A] !! U = Return(interpreter[U](ma))
  }
}


trait Handler_exports {
  type >>>![H1 <: Handler, H2 <: Handler] = H2 <<<! H1

  type <<<![H1 <: Handler, H2 <: Handler] = Handler {
    type Effects = H1#Effects with H2#Effects
    type Result[A] = H1#Result[H2#Result[A]]
  }

  //@#@TODO swap (A, S)
  // implicit class HandlerIntoPairExtension[S, U](val thiz: Handler.Apply[(S, ?), U]) {
  implicit class HandlerIntoPairExtension[S, U](val thiz: Handler.Apply[(?, S), U]) {
    type Const[X] = S
    type Identity[X] = X

    def eval: Handler.Apply[Identity, U] = thiz.map(new ((?, S) ~> Identity) {
      def apply[A](pair: (A, S)) = pair._1
      // def apply[A](pair: (S, A)) = pair._2
    })

    def exec: Handler.Apply[Const, U] = thiz.map[Const](new ((?, S) ~> Const) {
      def apply[A](pair: (A, S)) = pair._2
      // def apply[A](pair: (S, A)) = pair._1
    })

    def justState = exec
    def dropState = eval
  }
}
