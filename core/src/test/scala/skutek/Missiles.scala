package skutek
import org.specs2._


trait CanLaunchTheMissiles { this: Specification => 

  case class Missiles() { 
    private var count = 0 
    def launch_! = Eval { count += 1 }
    def launchedOnce = count == 1
    def mustHaveLaunchedOnce = count must_== 1
    def mustNotHaveLaunched = count must_== 0
  }
}
