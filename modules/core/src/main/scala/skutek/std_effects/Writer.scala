package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.custom_effect._
import skutek.utils.Accumulator


trait Writer[W] extends EffectImpl with AccumulatingEffect[W] {
  case class Tell(value: W) extends Op[Unit]

  final override type HandlerCtor[S] = CommonHandler[S]
  final override def handlerCtor[S](acc: Accumulator[W, S]) = new CommonHandler[S](acc)

  class CommonHandler[S](acc: Accumulator[W, S]) extends Stateless with Parallel with AlmostStateful[S] {
    final override def onReturn[A, U](a: A): A !@! U =
      Return((a, acc.zero))

    final override def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Tell(w) =>
          k(()).map {
            case (b, s) => (b, acc.add(acc.one(w), s))
          }
      }

    final override def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case ((a, s1), (b, s2)) =>
          val s12 = acc.add(s1, s2)
          k((a, b)).map {
            case (c, s3) =>
              val s123 = acc.add(s12, s3)
              (c, s123)
          }
      }
  }
}
