package skutek_experimental
import skutek.abstraction._
import skutek.abstraction.custom_effect._
import skutek_experimental._internals._


trait CyclicMemoizer[K, V] extends EffectImpl {
  case class Recur(key: K) extends Op[() => V]


  def handler[W] = new HandlerApply[W]
  class HandlerApply[W] {
    def apply(fun: K => V !! W with ThisEffect) = new Handler[W](Cache.empty[K, V], fun).tieKnots
  }


  protected class Handler[W](
    val initial: Cache[K, V], 
    fun: K => V !! W with ThisEffect
  ) extends Stateful2[Cache[K, V]] with Sequential {

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Recur(key) => cache => 
          cache.contents.get(key) match {
            case Some(o) => k(o)(cache)
            case None => {
              val o = new OnceVar[V]
              k(o)(cache.add(key, o))
            }
          }
      }

    //@#@TODO copied from State
    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      onProductDefault(ma, mb, k)


    def tieKnots: Into[Lambda[A => A !! W]] = new Into[Lambda[A => A !! W]] {
      def apply[A](pair: (A, Cache[K, V])): A !! W = {
        val (a, m) = pair
        if (m.untied.isEmpty)
          Return(a)
        else {
          val effs = 
            m.untied/*.iterator*/.map { case (key, o) => fun(key).map(v => o.tie(v)) }
            .traverseVoidShort.map(_ => a)
          val m2 = new Cache[K, V](m.contents)
          val h = new Handler[W](m2, fun)
          h.tieKnots.handle[W](effs).flatten
        }
      }
    }
  }
}
