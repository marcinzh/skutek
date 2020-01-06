package skutek_examples.sat_solver
import skutek._


sealed trait AST
case class Var(name: String) extends AST
case class Not(arg: AST) extends AST
case class And(lhs: AST, rhs: AST) extends AST
case class Or(lhs: AST, rhs: AST) extends AST
case class Imply(lhs: AST, rhs: AST) extends AST
case class Equiv(lhs: AST, rhs: AST) extends AST
