package skutek
import org.specs2._
import scala.util.Try 


class StackSafetyTests extends Specification {
  def is = Tramp.is ^ WideChoice.is ^ Repeatedly.is

  val TooBigForStack = 100000
  def mustNotStackOverflow[A](a : => A) = Try { a } must beSuccessfulTry
    


  object Tramp {
    def isEven(xs: List[Int]): Boolean !! Any =
      if (xs.isEmpty) Return(true) else Trampoline { isOdd(xs.tail) }

    def isOdd(xs: List[Int]): Boolean !! Any =
      if (xs.isEmpty) Return(false) else Trampoline { isEven(xs.tail) }

    def is = br ^ "Mutually recursive tail calls using Trampoline should be stack safe" ! mustNotStackOverflow {
      isEven((1 to TooBigForStack).toList).run 
    }
  }


  object WideChoice {
    def is = br ^ "Choice from big collection should be stack safe" ! mustNotStackOverflow {
      (for {
        i <- Choose(1 to TooBigForStack)
      } yield i)
      .runWith(ChoiceHandler) 
    }
  }


  object Repeatedly {
    case class Case[A, U](name: String, h: Handler { type Effects = U }, eff: A !! U) {
      def mapEff[B](f: A !! U => B !! U) = copy(eff = f(eff))
      def run = h run eff
    }
    
    val cases = List(
      Case("Reader", ReaderHandler(0), Ask[Int]),
      Case("Writer", WriterHandler.seq[Int], Tell(111)),
      Case("State", StateHandler(0), for { x <- Get[Int]; _ <- Put(x + 1) } yield ()),
      Case("Choice", ChoiceHandler, Choose(List(0)))
    )

    abstract class Mapper(val name: String) {
      def apply[A, U](eff: A !! U): _ !! U
    }

    val mappers = {
      def many[A, U](eff: A !! U) = Vector.fill(TooBigForStack)(eff)
      List(
        new Mapper("chained") { 
          def apply[A, U](eff: A !! U) = many(eff).reduce((a, b) => a.flatMap(_ => b)) 
        },
        new Mapper("traversed") { 
          def apply[A, U](eff: A !! U) = many(eff).traverse 
        }
      )
    }

    def is = 
      (for {
        c <- cases
        m <- mappers
        c2 = c.mapEff(m(_))
        name = s"${c.name} effect repeatedly ${m.name} should be stack safe"
        test = br ^ name ! mustNotStackOverflow { c2.run }
      } yield test)
      .reduce(_ ^ _)
  }
}
