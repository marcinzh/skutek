package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.effect._


trait Choice extends FilterableEffect {
  case class Choose[A](values: Iterable[A]) extends Operation[A]
  val NoChoice = Choose(Iterable.empty[Nothing])

  def from[A](as: A*) = Choose(as)

  def handler = findAll
  def findAll = DefaultChoiceHandler.findAll(this)
  def findFirst = DefaultChoiceHandler.findFirst(this)
}


object DefaultChoiceHandler {
  def findAll[Fx <: Choice](fx: Fx) = new fx.Nullary with fx.Parallel {
    type Result[A] = Vector[A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Vector(a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Choose(as) => iterate(as, k)
      }

    val onFail = Some(fx.NoChoice)

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap {
        case (as, bs) =>
          val abs = for {
            a <- as
            b <- bs
          } yield (a, b)
          iterate(abs, k)
      }

    def iterate[A, B, U](as: Iterable[A], k: A => B !@! U): B !@! U = {
      def loop(as: Iterable[A]): B !@! U = 
        as.size match {
          case 0 => Return(Vector())
          case 1 => k(as.head)
          case n =>
            val (as1, as2) = as.splitAt(n / 2)
            (loop(as1) *! loop(as2)).map {
              case (bs1, bs2) => bs1 ++ bs2
            }
        }
      loop(as)
    }
  }


  def findFirst[Fx <: Choice](fx: Fx) = new fx.Nullary with fx.Parallel {
    type Result[A] = Option[A]

    def onReturn[A, U](a: A): A !@! U =
      Return(Some(a))

    def onOperation[A, B, U](op: Operation[A], k: A => B !@! U): B !@! U =
      op match {
        case fx.Choose(as) => iterate(as, k)
      }

    val onFail = Some(fx.NoChoice)

    def onProduct[A, B, C, U](tma: A !@! U, tmb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (tma *! tmb).flatMap {
        case (Some(a), Some(b)) => k((a, b))
        case _ => Return(None)
      }

    def iterate[A, B, U](as: Iterable[A], k: A => B !@! U): B !@! U = {
      def loop(as: Iterable[A]): B !@! U =
        as.size match {
          case 0 => Return(None)
          case 1 => k(as.head)
          case n =>
            val (as1, as2) = as.splitAt(n / 2)
            (loop(as1) *! loop(as2)).map {
              case (bs1, bs2) => bs1 orElse bs2 //@#@YOLO
            }
        }
      loop(as)
    }
  }
}
