package skutek_experimental
import skutek.abstraction._
import skutek.abstraction.effect._
import skutek_experimental._internals._


trait CyclicMemoizer[K, V] extends EffectImpl {
  case class Recur(key: K) extends Op[() => V]


  def handler[W] = new HandlerApply[W]
  class HandlerApply[W] {
    def apply(fun: K => V !! W with ThisEffect) = new Handler[W](fun).tieKnots(Cache.empty[K, V])
  }


  protected class Handler[W](
    fun: K => V !! W with ThisEffect
  ) extends Unary[Cache[K, V]] with Sequential {
    type Result[A] = (A, Cache[K, V])

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
          cache.contents.get(key) match {
            case Some(o) => k(o)(cache)
            case None =>
              val o = new OnceVar[V]
              k(o)(cache.add(key, o))
          }
      }

    def tieKnots(initial: Cache[K, V]): Handler.Apply[? !! W, ThisEffect] = {
      val h0 = apply(initial)
      h0.map[? !! W](new h0.Into[? !! W] {
        def apply[A](pair: (A, Cache[K, V])): A !! W = {
          val (a, m) = pair
          if (m.untied.isEmpty)
            Return(a)
          else {
            val effs =
              m.untied/*.iterator*/.map { case (key, o) => fun(key).map(v => o.tie(v)) }
              .traverseVoidShort.map(_ => a)
            val m2 = new Cache[K, V](m.contents)
            val h = new Handler[W](fun)
            h.tieKnots(m2).handle[W](effs).flatten
          }
        }
      })
    }
  }
}
