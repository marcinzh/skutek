package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait Reader[R] extends Effect {
  case object Ask extends Operation[R]

  def Asks[A](f: R => A) = Ask.map(f)
  def Local[A, U](r: R)(scope: A !! U) = LocalMod(_ => r)(scope)
  def LocalMod[A, U](f: R => R)(scope: A !! U) = Ask.flatMap(r => handler(f(r)).handle[U](scope))

  def handler(r: R) = new Nullary with Parallel {
    type Result[A] = A

    def onReturn[A, U](a: A): A !@! U =
      Return(a)

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Ask => k(r)
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap(k)
  }
}
