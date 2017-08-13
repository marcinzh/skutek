package skutek
import org.specs2._


private case object TagA
private case object TagB

class TaggingTests extends Specification {

  def is = justOne ^ mixed ^ oneEffectTwoTags


  def justOne = br ^ "Single effect tagged" ! {
    val eff = for {
      _ <- Tell("foo") @! TagA
      _ <- Tell("bar") @! TagA
    } yield ()

    val h = WriterHandler.strings.exec @! TagA
    
    (h run eff) must_== Vector("foo", "bar")
  }


  def mixed = br ^ "Composition of tagged and untagged version of the same effect" ! {
    val eff = for {
      _ <- Tell("foo")
      _ <- Tell("bar") @! TagA
    } yield ()

    val h1 = WriterHandler.strings
    val h2 = WriterHandler.strings @! TagA
    
    (((h1 +! h2) run eff) must_== ((((), Vector("bar")), Vector("foo")))) and
    (((h2 +! h1) run eff) must_== ((((), Vector("foo")), Vector("bar"))))
  }


  def oneEffectTwoTags = br ^ "Composition of 2 instances of a single effect, each with unique tag" ! {
    val eff = for {
      s <- Get[String] @! TagA
      i <- Get[Int] @! TagB
      _ <- Put("Hello".take(i)) @! TagA
      _ <- Put(s.size) @! TagB
    } yield ()

    val h1 = StateHandler("qwerty") @! TagA
    val h2 = StateHandler(4) @! TagB

    (((h1 +! h2) run eff) must_== ((((), 6), "Hell"))) and
    (((h2 +! h1) run eff) must_== ((((), "Hell"), 6)))
  }
}
