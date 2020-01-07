package skutek.misc
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class ParallelismTest extends Specification {
  def is = ValidationWithWriter.is ^ ValidationWithState.is

  object ValidationWithWriter {
    case object FxW extends Writer[Vector[Int]]
    case object FxV extends Validation[Vector[Char]]

    val eff = for {
      _ <- FxW.Tell(1)
      _ <- FxW.Tell(2) *! FxV.Invalid('x') *! FxV.Invalid('y') *! FxW.Tell(3) *! FxV.Invalid('z')
      _ <- FxW.Tell(4)
    } yield ()

    val vh = FxV.handler
    val wh = FxW.handler

    def testWV = ((wh <<<! vh) run eff) must_== ((Left(Vector('x', 'y', 'z')), 1 to 3))
    def testVW = ((vh <<<! wh) run eff) must_== Left(Vector('x', 'y', 'z'))

    def is = 
      br ^ t ^ "Composition of 2 parallel effects:" ^
      br ^ "Validation handled before Writer, should be parallel" ! testWV ^
      br ^ "Writer handled before Validation, should be parallel" ! testVW ^
      bt
  }


  object ValidationWithState {
    case object FxS extends State[Int]
    case object FxV extends Validation[Vector[Char]]

    val eff = for {
      _ <- FxS.Put(111) *! FxV.Invalid('x') *! FxV.Invalid('y') *! FxS.Put(222) *! FxV.Invalid('z')
      _ <- FxS.Put(333) *! FxV.Invalid('w')
    } yield ()

    val vh = FxV.handler
    val sh = FxS.handler(0).dropState

    def testSV = ((sh <<<! vh) run eff) must_== Left(Vector('x', 'y', 'z'))
    def testVS = ((vh <<<! sh) run eff) must_== Left(Vector('x'))

    def is = 
      br ^ t ^ "Composition of a parallel effect with a sequential effect:" ^ 
      br ^ "Validation, when handled before State, should remain parallel" ! testSV ^
      br ^ "Validation, when handled after State, should be sequential"  ! testVS ^
      bt
  }
}
