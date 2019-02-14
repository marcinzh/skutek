package skutek.stack_safety
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._
import scala.util.Try 


trait CanStackOverflow { this: Specification => 
  val TooBigForStack = 100000

  def mustNotStackOverflow[A](a : => A) = Try { a } must beSuccessfulTry
}
