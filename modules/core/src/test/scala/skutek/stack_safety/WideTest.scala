package skutek.stack_safety
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class WideTest extends Specification with CanStackOverflow {
  def is = choice

  def choice = br ^ "Choice from big collection should be stack safe" ! mustNotStackOverflow {
    case object Fx extends Choice
    
    (for {
      i <- Fx.Choose(1 to TooBigForStack)
    } yield i)
    .runWith(Fx.handler) 
  }
}
