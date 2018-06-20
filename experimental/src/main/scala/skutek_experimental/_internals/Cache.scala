package skutek_experimental
package _internals


class Cache[K, V](
  val map: Map[K, OnceVar[V]],
  val untied: List[(K, OnceVar[V])] = Nil
) {
  def add(key: K, once: OnceVar[V]) = {
    val kv = (key, once)
    new Cache[K, V](
      map = map + kv, 
      untied = untied :+ kv
    )
  }

  def snapshot() = map.mapValues(f => f())
}


object Cache {
  def empty[K, V] = new Cache[K, V](Map(), Nil)
}
