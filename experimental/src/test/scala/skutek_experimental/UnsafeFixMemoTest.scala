package skutek_experimental
import skutek.abstraction._
import org.specs2._

class UnsafeFixMemoTest extends Specification with CanLaunchTheMissiles {

  def fibs(n: Int): (Int, Vector[Missile]) = {
    val missiles = Vector.fill(n + 1)(Missile())
    
    def fib = UnsafeFixMemo[Int, Int, Any]{ recur => 
      i => missiles(i).launch_! *>! (
        if (i <= 1) 
          Return(i)
        else
          for {
            a <- recur(i - 1) 
            b <- recur(i - 2)
            c = a + b
          } yield c
      )
    }

    (fib(n).run, missiles)
  }

  def is = {
    val (n, missiles) = fibs(10)
    (n must_== 55) and 
    missiles.map(_.mustHaveLaunchedOnce).reduce(_ and _)
  }
}
