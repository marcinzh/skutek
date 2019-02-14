package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.custom_effect._
import skutek.abstraction.custom_effect.{PrimitiveHandlerImpl => H}


trait Writer[W] extends EffectImpl {
  case class Tell(value: W) extends Op[Unit]


  abstract class CommonHandler[S](s0: S) extends Stateful2[S] with Parallel {
    def add(s1: S, s2: S): S
    def add1(s: S, w: W): S

    final def initial = s0

    final def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      op match {
        case Tell(w) => s => k(())(add1(s, w))
      }

    final def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      s1 => (ma(s0) *! mb(s0)).flatMap {
        case ((a, s2), (b, s3)) =>
          val s12 = add(s1, s2)
          val s123 = add(s12, s3)
          k((a, b))(s123)
      }
  }


  def handler = intoSeq

  def intoSeq = new CommonHandler(Vector.empty[W]) {
    def add(ws1: Vector[W], ws2: Vector[W]) = ws1 ++ ws2
    def add1(ws: Vector[W], w: W) = ws :+ w
  }

  def intoSet = new CommonHandler(Set.empty[W]) {
    def add(ws1: Set[W], ws2: Set[W]) = ws1 | ws2
    def add1(ws: Set[W], w: W) = ws + w
  }

  def fold(zero: W)(op: (W, W) => W) = new CommonHandler[W](zero) {
    def add(w1: W, w2: W) = op(w1, w2)
    def add1(w1: W, w2: W) = op(w1, w2)
  }

  def reduceOption(op: (W, W) => W) = new CommonHandler[Option[W]](None) {
    def add(mx: Option[W], my: Option[W]) = mx.flatMap(x => my.map(y => op(x, y)))
    def add1(mw: Option[W], w: W): Option[W] = mw.map(op(_: W, w))
  }
}
