package skutek_examples.sat_solver
import skutek.abstraction._
import skutek.std_effects._
import Solve.Fx

/*
 * Ported from OCaml code:
 * https://gist.github.com/Drup/4dc772ff82940608834fc65e3b80f583
 * The control flow mechanism by (ab)use of exceptions from the original, is replaced by Choice effect
 */


object Solve {
  case object Fx extends Choice
  type Fx = Fx.type

  def apply(formula: List[List[Literal]]): Set[Literal] !! Fx = Env(Set(), formula).unsat
}

case class Literal(name: String, yes: Boolean) {
  def unary_~ = copy(yes = !yes)
}

private case class Env(solution: Set[Literal], formula: List[List[Literal]]) {

  def assume(l: Literal): Env !! Fx = 
    if (solution.contains(l)) 
      Return(this)
    else
      copy(solution = solution + l).bcp

  def bcp = formula.foldLeft_!!(copy(formula = Nil))((env, ls) => env.bcpAux(ls))

  def bcpAux(ls: List[Literal]): Env !! Fx = 
    if (ls.exists(l => solution.contains(l)))
      Return(this)
    else
      ls.filter(l => !solution.contains(~l)) match {
        case List() => Fx.NoChoice
        case List(l) => assume(l)
        case ls2 => Return(copy(formula = ls2 :: formula))
      }

  def unsat: Set[Literal] !! Fx = 
    formula match {
      case Nil => Return(solution)
      case (l :: _ ) :: _ => for {
        l2 <- Fx.from(l, ~l)
        env <- assume(l2)
        result <- env.unsat
      } yield result
      case _ => ???
    }
}
