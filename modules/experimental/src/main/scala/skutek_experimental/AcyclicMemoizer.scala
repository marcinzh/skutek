package skutek_experimental
import skutek.abstraction._
import skutek.abstraction.effect._
import skutek_experimental._internals._


trait AcyclicMemoizer[K, V] extends EffectImpl {
  case class Recur(key: K) extends Op[V]


  def handler[W] = new HandlerApply[W]
  class HandlerApply[W] {
    def apply(fun: K => V !! W with ThisEffect) = new Handler[W](Map.empty[K, V], fun).dropState
  }


  protected class Handler[W](
    val initial: Map[K, V],
    fun: K => V !! W with ThisEffect
  ) extends Stateful[Map[K, V]] with Sequential {
    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Recur(key) => cache =>
          cache.get(key) match {
            case Some(v) => k(v)(cache)
            case None =>
              fun(key)
              .handleWith[U with W](new Handler[W](cache, fun))
              .flatMap { case (v, cache2) =>
                val cache3 = cache2.updated(key, v)
                k(v)(cache3)
              }
              .downCast[U] //// cheat: casts `W with U` to `U`
          }
      }
  }
}
