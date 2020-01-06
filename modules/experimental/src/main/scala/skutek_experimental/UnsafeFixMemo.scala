package skutek_experimental
import skutek.abstraction._


object UnsafeFixMemo {
  def apply[K, V, U] = new Apply[K, V, U]

  class Apply[K, V, U] {
    private type F = K => V !! U
    private type FF = F => F

    def apply(ff: FF): F = {
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
}
