package skutek_examples.sat_solver
import skutek.abstraction._
import skutek.std_effects._


object Parser {
  case object ErrorFx extends Except[(String, Int)]

  def apply(source: String): AST !! ErrorFx.type = 
    (for {
      ast <- parseExpr
      c <- getChar
      _ <- if (c == EOF) Return else fail("Expected end of input")
    } yield ast)
    .handleWith[ErrorFx.type](StFx.handler(ParserState(0, source)).dropState)

  case object StFx extends State[ParserState]
  type Parser[T] = T !! StFx.type with ErrorFx.type
  case class ParserState(position: Int, source: String)
  val EOF = '\u0000'

  val getChar: Parser[Char] = for {
    ps <- StFx.Get
    _ <- StFx.Put(ps.copy(position = ps.position + 1))
    c <- 
      if (!ps.source.isDefinedAt(ps.position)) Return(EOF) else {
        val c = ps.source(ps.position) 
        if (c.isWhitespace) getChar else Return(c)
      }
  } yield c

  val ungetChar = StFx.Mod { case ParserState(i, cs) => ParserState(i - 1, cs) }
  
  def fail(msg: String) = StFx.Get.flatMap { case ParserState(i, _) => ErrorFx.Raise((msg, i - 1)) }

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
          case ')' => Return
          case _ => fail("Expected closing brace")
        }
      } yield expr
      case ')' => fail("Missing opening brace")
      case EOF => fail("Unexpected end of input")
      case _ => fail("Expected start of expression")
    }
  } yield expr
}
