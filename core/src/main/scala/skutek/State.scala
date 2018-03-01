package skutek
import _internals._

sealed trait State[S]
object State extends EffectCompanion1[State]

sealed trait StateOperation[A, S] extends Operation[A, State[S]]
case class Get[S]() extends StateOperation[S, S] 
case class Put[S](value: S) extends StateOperation[Unit, S]

case class Modify[S](fun: S => S) extends SyntheticOperation.Shallow[Unit, State[S]] {
  def synthesize[T <: SyntheticTagger](implicit tagger: T): Unit !! tagger.Tagged[State[S]] = 
    for {
      s <- Get[S].tagged
      _ <- Put(fun(s)).tagged
    } yield ()
}


case class StateHandler[S](val initial: S) extends StatefulHandler2[State[S]] {
  type Stan = S
  type Op[A] = StateOperation[A, S]

  def onOperation[A, B, U](op: Op[A], k: A => Secret[B, U]): Secret[B, U] = op match {
    case _: Get[_]  => s => k(s)(s)
    case Put(s2)     => _ => k(())(s2)
  }

  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U], k: ((A, B)) => Secret[C, U]): Secret[C, U] =
    (s1: Stan) => aa(s1).flatMap { 
      case (a, s2) => bb(s2).flatMap { 
        case (b, s3) => k((a, b))(s3)
      }
    }
}
