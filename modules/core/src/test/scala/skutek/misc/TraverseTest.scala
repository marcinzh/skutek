package skutek.misc
import skutek.abstraction._
import skutek.std_effects._
import skutek.operations.CanLaunchTheMissiles
import org.specs2._


class TraverseTest extends Specification with CanLaunchTheMissiles {
  def is = TravV.is ^ TravE.is

  object TravV {
    case object Fx extends Validation[Vector[String]]

    def mkEffs = {
      val m = Missile()
      val effs = Vector(Fx.Invalid("foo"), m.launch_!, Fx.Invalid("bar"))
      (effs, m)
    }

    def is = 
      (br ^ "Parallel traverse of Validation" ! {
        val (effs, m) = mkEffs
        (Fx.handler run effs.traverse) must_== Left(Vector("foo", "bar")) and
        m.mustHaveLaunchedOnce
      }) ^
      (br ^ "Sequential traverse of Validation" ! {
        val (effs, m) = mkEffs
        (Fx.handler run effs.traverseShort) must_== Left(Vector("foo")) and
        m.mustNotHaveLaunched
      })
  }


  object TravE {
    case object Fx extends Except[String]

    def mkEffs = {
      val m = Missile()
      val effs = Vector(Fx.Raise("foo"), m.launch_!)
      (effs, m)
    }

    def is = 
      (br ^ "Parallel traverse of Except" ! {
        val (effs, m) = mkEffs
        (Fx.handler run effs.traverse) must_== Left("foo") and
        m.mustHaveLaunchedOnce
      }) ^
      (br ^ "Sequential traverse of Except" ! {
        val (effs, m) = mkEffs
        (Fx.handler run effs.traverseShort) must_== Left("foo") and
        m.mustNotHaveLaunched
      })
  }
}
