package skutek_experimental
import skutek.abstraction.{!!, Return}
import skutek.abstraction.effect.Effect
import skutek_experimental._internals._


trait AcyclicMemoizer[K, V] extends Effect {
  case class Recur(key: K) extends Operation[V]


  def handler[W] = new HandlerApply[W]
  class HandlerApply[W] {
    def apply(fun: K => V !! W with ThisEffect) = new Handler[W](fun)(Map.empty[K, V]).dropState
  }


  protected class Handler[W](
    fun: K => V !! W with ThisEffect
  ) extends Unary[Map[K, V]] with Sequential {
    type Result[A] = (Map[K, V], A)

    def onReturn[A, U](a: A): A !@! U =
      s => Return((s, a))

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => tma(s1).flatMap {
        case (s2, a) => tmb(s2).flatMap {
          case (s3, b) => k((a, b))(s3)
        }
      }

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Recur(key) => cache =>
          cache.get(key) match {
            case Some(v) => k(v)(cache)
            case None =>
              fun(key)
              .handleWith[U with W](new Handler[W](fun)(cache))
              .flatMap { case (cache2, v) =>
                val cache3 = cache2.updated(key, v)
                k(v)(cache3)
              }
              .downCast[U] //// cheat: casts `W with U` to `U`
          }
      }
  }
}
