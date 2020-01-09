package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait State[S] extends Effect {
  case object Get extends Operation[S]
  case class Put(value: S) extends Operation[Unit]

  def Mod(f: S => S) = Get.flatMap(s => Put(f(s)))

  def handler = new Unary[S] with Sequential {
    type Result[A] = (A, S)

    def onReturn[A, U](a: A): A !@! U =
      s => Return((a, s))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Get => s => k(s)(s)
        case Put(s) => _ => k(())(s)
      }

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => tma(s1).flatMap {
        case (a, s2) => tmb(s2).flatMap {
          case (b, s3) => k((a, b))(s3)
        }
      }
  }
}
