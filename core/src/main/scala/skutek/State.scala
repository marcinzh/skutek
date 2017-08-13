package skutek
import _internals._

sealed trait State[S]
object State extends UnaryEffectCompanion[State[?]]

sealed trait StateOperation[A, S] extends Operation[A, State[S]]
case class Get[S]() extends StateOperation[S, S] 
case class Put[S](value: S) extends StateOperation[Unit, S]

case class Modify[S](fun: S => S) extends SyntheticOperation.Shallow[Unit, State[S]] {
  def synthesize[T <: SyntheticTagger](implicit tagger: T) = 
    for {
      s <- Get[S].tagged
      _ <- Put(fun(s)).tagged
    } yield ()
}


case class StateHandler[S](val initial: S) extends SimpleStatefulHandler[State[S]] {
  type Op[A] = StateOperation[A, S]
  type Stan = S

  def onOperation[A](op: Op[A], s: S): (A, S) = op match {
    case _: Get[_]  => (s, s)
    case Put(s2)     => ((), s2)
  }
}
