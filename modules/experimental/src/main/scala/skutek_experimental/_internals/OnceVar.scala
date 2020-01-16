package skutek_experimental
package _internals


class OnceVar[A] extends Function0[A] {
  var tied = false
  var result: A = null.asInstanceOf[A]

  def :=(a: A) = tie(a)

  def tie(a: A): Unit = {
    if (!tied) {
      result = a
      tied = true
    }
    else
      sys.error("@#@ already resolved")
  }

  override def apply(): A = 
    if (tied) 
      result 
    else
      sys.error("@#@ unresolved")

}
