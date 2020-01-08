package skutek_experimental
import skutek.abstraction._
import skutek.abstraction.effect._
import skutek_experimental._internals._


trait AcyclicMemoizer[K, V] extends EffectImpl {
  case class Recur(key: K) extends Op[V]


  def handler[W] = new HandlerApply[W]
  class HandlerApply[W] {
    def apply(fun: K => V !! W with ThisEffect) = new Handler[W](fun)(Map.empty[K, V]).dropState
  }


  protected class Handler[W](
    fun: K => V !! W with ThisEffect
  ) extends Unary[Map[K, V]] with Sequential {
    type Result[A] = (A, Map[K, V])

    def onReturn[A, U](a: A): A !@! U =
      s => Return((a, s))

    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => ma(s1).flatMap {
        case (a, s2) => mb(s2).flatMap {
          case (b, s3) => k((a, b))(s3)
        }
      }

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Recur(key) => cache =>
          cache.get(key) match {
            case Some(v) => k(v)(cache)
            case None =>
              fun(key)
              .handleWith[U with W](new Handler[W](fun)(cache))
              .flatMap { case (v, cache2) =>
                val cache3 = cache2.updated(key, v)
                k(v)(cache3)
              }
              .downCast[U] //// cheat: casts `W with U` to `U`
          }
      }
  }
}
