package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait State[S] extends EffectImpl {
  case object Get extends Op[S]
  case class Put(value: S) extends Op[Unit]

  def Mod(f: S => S) = Get.flatMap(s => Put(f(s)))

  def handler(s0: S) = new Stateful[S] with Sequential {
    def initial = s0

    def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Get => s => k(s)(s)
        case Put(s) => _ => k(())(s)
      }
  }
}
