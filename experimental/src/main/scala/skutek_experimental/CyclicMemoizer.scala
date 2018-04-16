package skutek_experimental
import skutek._
import skutek._internals._
import skutek_experimental._internals._

//// Unacceptable solution, because its impossible to tag the handler

sealed trait CyclicMemoizer[K, V]
object CyclicMemoizer extends EffectCompanion2[CyclicMemoizer]

sealed trait CyclicMemoizerOperation[A, K, V] extends Operation[A, CyclicMemoizer[K, V]]
class CyclicMemo[K, V](val key: K) extends CyclicMemoizerOperation[() => V, K, V]
object CyclicMemo {
  def apply[V] = new Apply[V]
  class Apply[V] {
    def apply[K](key: K) = new CyclicMemo[K, V](key)
  }
}


object CyclicMemoizerHandler {
  def apply[W] = new Apply[W]
  class Apply[W] {
    def apply[K, V](fun: K => V !! W with CyclicMemoizer[K, V]) = 
      new CyclicMemoizerHandler[K, V, W](Cache.empty[K, V], fun).tieKnots
  }
}


class CyclicMemoizerHandler[K, V, W](val initial: Cache[K, V], fun: K => V !! W with CyclicMemoizer[K, V]) extends StatefulHandler2[CyclicMemoizer[K, V]] {
  type Op[A] = CyclicMemoizerOperation[A, K, V]
  type Stan = Cache[K, V]

  def onOperation[A, B, U](op: Op[A]): Cont[A, B, U] = 
    k => m => op match {
      case op: CyclicMemo[K, V] => m.map.get(op.key) match {
        case Some(o) => k(o)(m)
        case None => {
          val o = new OnceVar[V]
          k(o)(m.add(op.key, o))
        }
      }
    }

  private def tieKnots: MappedHandler[Lambda[A => A !! W]] = new MappedHandler[Lambda[A => A !! W]] {
    def apply[A](pair: (A, Stan)): A !! W = {
      val (a, m) = pair
      if (m.untied.isEmpty)
        Return(a)
      else {
        val effs = 
          m.untied/*.iterator*/.map { case (key, o) => fun(key).map(v => o.tie(v)) }
          .seriallyVoid.map(_ => a)
        val m2 = new Cache[K, V](m.map)
        val h = new CyclicMemoizerHandler[K, V, W](m2, fun)
        h.tieKnots.handleCarefully[W](effs).flatten
      }
    }
  }
}
