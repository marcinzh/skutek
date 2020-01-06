package skutek_experimental
package _internals


class Cache[K, V](
  val contents: Map[K, OnceVar[V]],
  val untied: List[(K, OnceVar[V])] = Nil
) {
  def add(key: K, once: OnceVar[V]) = {
    val kv = (key, once)
    new Cache[K, V](
      contents = contents + kv, 
      untied = untied :+ kv
    )
  }

  def snapshot() = contents.mapValues(f => f())
}


object Cache {
  def empty[K, V] = new Cache[K, V](Map(), Nil)
}
