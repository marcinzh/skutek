package skutek_experimental
import skutek._

object UnsafeFixMemo {
  type WithRecur[F] = F => F

  def apply[K, V, U](ff: WithRecur[K => V !! U]): K => V !! U = {
    val m = collection.mutable.HashMap.empty[K, V]
    def f = ff(recur)
    def recur(k: K): V !! U = {
      m.get(k) match {
        case Some(v) => Return(v)
        case None => 
          for {
            v <- Trampoline { f(k) }
            _ = m(k) = v
          } yield v
      }
    }
    recur
  }
}
