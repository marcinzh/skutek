package skutek_examples.sat_solver
import skutek._


object Parser {

  def apply(source: String): AST !! Error[(String, Int)] = 
    (for {
      ast <- parseExpr
      c <- getChar
      _ <- if (c == EOF) Return() else fail("Expected end of input")
    } yield ast)
    .handleCarefullyWith(StateHandler(ParserState(0, source)).eval)

  val EOF = '\u0000'
  type Parser[T] = T !! State[ParserState] with Error[(String, Int)]
  case class ParserState(position: Int, source: String)

  val getChar: Parser[Char] = for {
    ps <- Get[ParserState]
    _ <- Put(ps.copy(position = ps.position + 1))
    c <- 
      if (!ps.source.isDefinedAt(ps.position)) Return(EOF) else {
        val c = ps.source(ps.position) 
        if (c.isWhitespace) getChar else Return(c)
      }
  } yield c

  val ungetChar = Modify[ParserState]{ case ParserState(i, cs) => ParserState(i - 1, cs) }
  
  def fail(msg: String) = Get[ParserState].flatMap { case ParserState(i, _) => Wrong((msg, i - 1)) }

  def parseExpr: Parser[AST] = 
    parseBinaryOrElse('=', Equiv)(
    parseBinaryOrElse('>', Imply)(
    parseBinaryOrElse('|', Or)(
    parseBinaryOrElse('&', And)(
    parseOther))))

  def parseBinaryOrElse(Op: Char, cons: (AST, AST) => AST)(parseArg: Parser[AST]): Parser[AST] = {
    def loop: Parser[AST] = for {
      lhs <- parseArg
      c <- getChar
      expr <- c match {
        case Op => loop.map(rhs => cons(lhs, rhs)) 
        case _ => ungetChar *>! Return(lhs)
      }
    } yield expr
    loop
  }

  def parseOther: Parser[AST] = for {
    char <- getChar
    expr <- char match {
      case '!' => parseOther.map(Not)
      case c if c.isLetter => Return(Var(c.toString))
      case '(' => for {
        expr <- parseExpr
        c <- getChar
        _ <- c match { 
          case ')' => Return()
          case _ => fail("Expected closing brace")
        }
      } yield expr
      case ')' => fail("Missing opening brace")
      case EOF => fail("Unexpected end of input")
      case _ => fail("Expected start of expression")
    }
  } yield expr
}
