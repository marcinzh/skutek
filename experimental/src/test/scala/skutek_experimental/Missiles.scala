package skutek_experimental
import skutek._
import org.specs2._

//// cloned from core/, until its fixed in sbt


trait CanLaunchTheMissiles { this: Specification => 

  case class Missiles() { 
    private var count = 0 
    def launch() = { count += 1 }
    def launch_! = Eval { launch() }
    def launchedOnce = count == 1
    def mustHaveLaunchedOnce = count must_== 1
    def mustNotHaveLaunched = count must_== 0
  }
}
