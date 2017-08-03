package skutek
import org.specs2._


class TraverseTests extends Specification with CanLaunchTheMissiles {
  def is = ValidationWithWriter.is ^ ValidationWithState.is ^ Trav1.is ^ TraverseLaziness.is

  sealed trait Invalids {
    val (x, y, z, w) = ("bad", "wrong", "incorrect", "unacceptable")
  }


  object ValidationWithWriter extends Invalids {
    val eff = for {
      _ <- Tell(1)
      _ <- Tell(2) *! Invalid(x) *! Invalid(y) *! Tell(3) *! Invalid(z)
      _ <- Tell(4)
    } yield ()

    val v = ValidationHandler[String]
    val h = WriterHandler.seq[Int]

    def testWV = ((h +! v) run eff) must_== ((Left(Vector(x, y, z)), 1 to 3))
    def testVW = ((v +! h) run eff) must_== Left(Vector(x, y, z)) 

    def is = 
      br ^ t ^ "Composition of 2 parallelizable effects:" ^ 
      br ^ "Writer on top of Validation should be parallelizable" ! testWV ^
      br ^ "Validation on top of Writer should be parallelizable" ! testVW ^
      bt
  }


  object ValidationWithState extends Invalids {
    val eff = for {
      _ <- Put(111) *! Invalid(x) *! Invalid(y) *! Put(222) *! Invalid(z)
      _ <- Put(333) *! Invalid(w)
    } yield ()

    val v = ValidationHandler[String]
    val s = StateHandler(0).eval

    def testSV = ((s +! v) run eff) must_== Left(Vector(x, y, z))
    def testVS = ((v +! s) run eff) must_== Left(Vector(x))

    def is = 
      br ^ t ^ "Composition of a parallelizable effect with a non parallelizable effect:" ^ 
      br ^ "Validation, when handled before State, should remain parallelizable" ! testSV ^
      br ^ "Validation, when handled after State, should not be parallelizable"  ! testVS ^
      bt
  }


  object Trav1 extends Invalids {

    val effs = Vector(Invalid(x), Invalid(y), Invalid(z))

    val h = ValidationHandler[String]

    def testParallel = (h run effs.parallelly) must_== Left(Vector(x, y, z))
    def testSerial   = (h run effs.serially)   must_== Left(Vector(x))

    def is = 
      br ^ "Parallel traverse of Validation" ! testParallel ^
      br ^ "Serial traverse of Validation"   ! testSerial
  }


  object TraverseLaziness {

    def is = 
      br ^ t ^ "Lazyness of traverse:" ^ 
      allTests ^
      bt

    def allTests = {
      val tf = List(true, false)
      (for {
        useVoid <- tf
        useLazy <- tf
        onTop <- tf
      } yield oneTest(useVoid, useLazy, onTop))
      .reduce(_ ^ _)
    }

    def oneTest(useVoid: Boolean, useLazy: Boolean, onTop: Boolean) = {
      val missiles = Missiles()
      val effs = (Tell(true) :: Naught :: missiles.launch_! :: Nil)
      val eff = (useVoid, useLazy) match {
        case (false, false) => effs.parallelly
        case (false, true) => effs.serially
        case (true, false) => effs.parallellyVoid
        case (true, true) => effs.seriallyVoid
      }

      val m = MaybeHandler
      val w = WriterHandler.seq[Boolean]
      if (onTop)
        (m +! w) run eff
      else
        (w +! m) run eff

      val descr = {
        val sLazy = if (useLazy) "parallelly" else "serially"
        val sLazy2 = if (useLazy) "eager" else "lazy"
        val sVoid = if (useVoid) "" else "Void"
        val sTop = if (onTop) "top" else "bottom"
              s"$sLazy$sVoid, when abortable effect is on the $sTop of the handler stack, should be $sLazy2"
          }

      br ^ descr ! { missiles.launchedOnce must_== !useLazy }
    }
  }
}
