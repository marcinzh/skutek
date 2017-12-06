package skutek
import _internals._

sealed trait Reader[S]
object Reader extends EffectCompanion1[Reader]

sealed trait ReaderOperation[A, S] extends Operation[A, Reader[S]]
case class Ask[S]() extends ReaderOperation[S, S]
private case class DontTell[S](s: S) extends ReaderOperation[Unit, S]


case class Asks[S, A](fun: S => A) extends SyntheticOperation.Shallow[A, Reader[S]] {
  def synthesize[T <: SyntheticTagger](implicit tagger: T): A !! tagger.Tagged[Reader[S]] = 
    Ask[S].tagged.map(fun)
}

object Asks {
  def apply[S] : Apply[S] = new Apply[S]()
  protected class Apply[S] {
    def apply[A](f: S => A) = new Asks(f)
  }
}

class LocalMod[A, S, U](f: S => S, eff: A !! U) extends SyntheticOperation.Deep[A, Reader[S], U] {
  def synthesize[T <: SyntheticTagger](implicit tagger: T): A !! tagger.Tagged[Reader[S]] with U = 
    for {
      s <- Ask[S].tagged
      _ <- DontTell(f(s)).tagged
      a <- eff
      _ <- DontTell(s).tagged
    } yield a
}

object LocalMod {
  def apply[S](f: S => S) = new Apply(f)
  protected class Apply[S](f: S => S) {
    def apply[A, U](eff: A !! U) = new LocalMod(f, eff)
  }
}

object Local {
  def apply[S](s: S) = LocalMod[S](_ => s)
}


case class ReaderHandler[S](val initial: S) extends StatefulHandler.NoSecret[Reader[S]] {
  type Op[A] = ReaderOperation[A, S]
  type Result[A] = A
  type Stan = S

  final def onReturn[A](a: A) = _ => Return(a)

  final def onOperation[A, B, U](op: Op[A], k: A => S => B !! U): S => B !! U = op match {
    case _: Ask[_] => s => k(s)(s)
    case DontTell(s) => _ => k(())(s)
  }

  final def onProduct[A, B, C, U](
    x: S => A !! U, 
    y: S => B !! U, 
    k: ((A, B)) => S => C !! U
  ): S => C !! U = 
    (s: S) => 
      (x(s) *! y(s)).flatMap { 
        case (a, b) => k((a, b))(s)
      }
}
