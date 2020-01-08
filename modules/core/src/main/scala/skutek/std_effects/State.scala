package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait State[S] extends EffectImpl {
  case object Get extends Op[S]
  case class Put(value: S) extends Op[Unit]

  def Mod(f: S => S) = Get.flatMap(s => Put(f(s)))

  def handler = new Unary[S] with Sequential {
    type Result[A] = (A, S)

    def onReturn[A, U](a: A): A !@! U =
      s => Return((a, s))

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Get => s => k(s)(s)
        case Put(s) => _ => k(())(s)
      }

    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => ma(s1).flatMap {
        case (a, s2) => mb(s2).flatMap {
          case (b, s3) => k((a, b))(s3)
        }
      }
  }
}
