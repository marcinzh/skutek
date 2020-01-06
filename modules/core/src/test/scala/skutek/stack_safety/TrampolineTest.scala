package skutek.stack_safety
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class TrampolineTest extends Specification with CanStackOverflow {
  def is = evenOdd

  def evenOdd = br ^ "Mutually recursive tail calls using Trampoline should be stack safe" ! {
    def isEven(xs: List[Int]): Boolean !! Any =
      if (xs.isEmpty) Return(true) else Trampoline { isOdd(xs.tail) }

    def isOdd(xs: List[Int]): Boolean !! Any =
      if (xs.isEmpty) Return(false) else Trampoline { isEven(xs.tail) }

    mustNotStackOverflow {
      isEven((1 to TooBigForStack).toList).run 
    }
  }
}
