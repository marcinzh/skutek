package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait Reader[S] extends EffectImpl {
  case object Ask extends Op[S]

  def Asks[A](f: S => A) = Ask.map(f)
  def Local[A, U](s: S)(scope: A !! U) = LocalMod(_ => s)(scope)
  def LocalMod[A, U](f: S => S)(scope: A !! U) = Ask.flatMap(s => handler(f(s)).handle[U](scope))

  def handler(s: S) = new Stateless with Parallel {
    type Result[A] = A

    def onReturn[A, U](a: A): A !@! U =
      Return(a)

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Ask => k(s)
      }

    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap(k)
  }
}
