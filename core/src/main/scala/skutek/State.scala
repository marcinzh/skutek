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

  def onOperation[A, B, U](op: Op[A]): Cont[A, B, U] = 
    k => s => op match {
      case _: Get[_]  => k(s)(s)
      case Put(s2)     => k(())(s2)
    }

  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U]): Cont[(A, B), C, U] =
    k => s => aa(s).flatMap { 
      case (a, s2) => bb(s2).flatMap { 
        case (b, s3) => k((a, b))(s3)
      }
    }
}
