package skutek_examples.sat_solver
import skutek._

/*
 * Ported from OCaml code:
 * https://gist.github.com/Drup/4dc772ff82940608834fc65e3b80f583
 * The control flow mechanism by (ab)use of exceptions from the original, is replaced by Choice effect
 */

object Solve {
  def apply(formula: List[List[Literal]]): Set[Literal] !! Choice = Env(Set(), formula).unsat
}

case class Literal(name: String, yes: Boolean) {
  def unary_~ = copy(yes = !yes)
}

private case class Env(solution: Set[Literal], formula: List[List[Literal]]) {

  def assume(l: Literal): Env !! Choice = 
    if (solution.contains(l)) 
      Return(this)
    else
      copy(solution = solution + l).bcp

  def bcp = formula.foldLeft(Return(copy(formula = Nil)).upCast[Choice]) { (env_!, ls) => env_!.flatMap(_.bcpAux(ls)) }

  def bcpAux(ls: List[Literal]): Env !! Choice = 
    if (ls.exists(l => solution.contains(l)))
      Return(this)
    else
      ls.filter(l => !solution.contains(~l)) match {
        case List() => NoChoice
        case List(l) => assume(l)
        case ls2 => Return(copy(formula = ls2 :: formula))
      }

  def unsat: Set[Literal] !! Choice = 
    formula match {
      case Nil => Return(solution)
      case (l :: _ ) :: _ => for {
        l2 <- Choose.from(l, ~l)
        env <- assume(l2)
        result <- env.unsat
      } yield result
      case _ => ???
    }
}