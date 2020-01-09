package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait Choice extends FilterableEffect {
  case class Choose[A](values: Iterable[A]) extends Operation[A]
  val NoChoice = Choose(Iterable.empty[Nothing])

  def from[A](as: A*) = Choose(as)

  def handler = findAll

  trait CommonHandler extends Nullary with Parallel {
    def zero[A] : Result[A]
    def one[A](a: A): Result[A]
    def plus[A](ma1: Result[A], ma2: Result[A]): Result[A]

    final def onReturn[A, U](a: A): A !@! U =
      Return(one(a))

    final def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case Choose(as) => iterate(as, k)
      }

    final val onFail = Some(NoChoice)

    protected def iterate[A, B, U](as: Iterable[A], k: A => B !@! U): B !@! U = {
      def loop(as: Iterable[A]): B !@! U = 
        as.size match {
          case 0 => Return(zero)
          case 1 => k(as.head)
          case n =>
            val (as1, as2) = as.splitAt(n / 2)
            (loop(as1) *! loop(as2)).map {
              case (bs1, bs2) => plus(bs1, bs2)
            }
        }
      loop(as)
    }
  }


  def findAll = new CommonHandler {
    type Result[A] = Vector[A]

    def zero[A] = Vector()
    def one[A](a: A) = Vector(a)
    def plus[A](ma1: Result[A], ma2: Result[A]) = ma1 ++ ma2


    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (as, bs) => 
          val abs = for {
            a <- as
            b <- bs
          } yield (a, b)
          iterate(abs, k)
      }
  }


  def findFirst = new CommonHandler {
    type Result[A] = Option[A]

    def zero[A] = None
    def one[A](a: A) = Some(a)
    def plus[A](ma1: Result[A], ma2: Result[A]) = ma1 orElse ma2

    def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma *! mb).flatMap {
        case (Some(a), Some(b)) => k((a, b))
        case _ => Return(None)
      }
  }
}
