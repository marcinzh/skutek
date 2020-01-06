package skutek_examples.sat_solver
import skutek._

/*
 * No effects to be seen here, move along :)
 */
 
object CNF {

  def convert(ast: AST): List[List[Literal]] = distrib(yes(elim(ast))).toList.map(_.toList)

  def elim(ast: AST): AST = ast match {
    case And(a, b) => And(elim(a), elim(b))
    case Or(a, b) => Or(elim(a), elim(b))
    case Imply(a, b) => Or(Not(elim(a)), elim(b))
    case Equiv(a, b) => { val c = elim(a); val d = elim(b); And(Or(Not(c), d), Or(c, Not(d))) }
    case a => a
  }

  def yes(ast: AST): AST = ast match {
    case Not(a) => not(a)
    case And(a, b) => And(yes(a), yes(b))
    case Or(a, b) => Or(yes(a), yes(b))
    case a => a
  }

  def not(ast: AST): AST = ast match {
    case Not(a) => yes(a)
    case And(a, b) => Or(not(a), not(b))
    case Or(a, b) => And(not(a), not(b))
    case a => Not(a)
  }

  def distrib(ast: AST): Set[Set[Literal]] = ast match {
    case Var(name) => Set(Set(Literal(name, true)))
    case Not(Var(name)) => Set(Set(Literal(name, false)))
    case And(a, b) => distrib(a) | distrib(b)
    case Or(a, b) => for { aa <- distrib(a); bb <- distrib(b) } yield aa | bb
    case _ => ???
  }
}
