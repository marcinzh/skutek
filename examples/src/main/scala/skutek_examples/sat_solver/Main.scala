package skutek_examples.sat_solver
import skutek._


object Main {

  def help() = println("""
    |Give logical formula constructed from: 
    |    single letter variables
    |    braces:      (a)
    |    negation:    !a       (highest precedence)
    |    conjunction: a & b    
    |    disjunction: a | b
    |    implication: a > b
    |    equivalence: a = b    (lowest precedence)
    |All binary operators are treated as left associative.
  """.stripMargin)


  def apply(args: Seq[String]) = args match {
    case Seq("help" | "-help" | "--help", _*) => help()
    case Seq() => runDefault()
    case _ => run(args.mkString(" "))
  }


  def runDefault() = {
    help()
    val formula = "a | b & !c | d "
    println("Running with default example:")
    println(formula)
    println()
    run(formula)
  }


  def run(formula: String) = parse(formula).foreach { case ast =>
    val cnf = CNF.convert(ast)

    { // I dare you to remove the blank line above
      val lines = for (ls <- cnf) yield {
        ls.toVector.sortBy(l => (l.name, l.yes)).map {
          case Literal(a, true) => a.toString
          case Literal(a, false) => "¬" + a.toString
        }.mkString("∧ (", " ∨ ", ")")
      }
      println("The formula after conversion to CNF:")
      for (line <- lines.sorted)
        println(line)
      println
    }

    val names = formula.filter(_.isLetter).distinct
    val substs = ChoiceHandler.run(
      for {
        result <- Solve(cnf)
        yays = result.collect { case Literal(x, true) => x.head }.toSet
        nays = result.collect { case Literal(x, false) => x.head }.toSet
        subst <- 
          (for {
            c <- names
            y = yays.contains(c)
            n = nays.contains(c)
            ys = if (y || !n) List('1') else Nil
            ns = if (n || !y) List('0') else Nil
          } yield Choose(ns ++ ys))
          .parallelly
          .map(_.mkString)
      } yield subst
    )

    if (substs.isEmpty)
      println("The formula is not satisfable.")
    else {
      println("The formula is satisfable with following substitutions:")
      val sep = "   "
      println(Console.UNDERLINED + names.mkString(" ") + Console.RESET + sep + Console.UNDERLINED + formula + Console.RESET)
      for (subst <- substs.sorted) yield {
        val f2 = formula.map(c => {
          val i = names.indexOf(c)
          if (i == -1) c else subst(i)
        })
        println(subst.mkString(" ") + sep + f2)
      }
    }
  }


  def parse(formula: String): Option[AST] = 
    Parser(formula).runWith(ErrorHandler[(String, Int)]) match {
      case Left((err, n)) => {
        println(s"Error: $err")
        val f = formula + " "
        val a = f.take(n)
        val b = f.drop(n).take(1)
        val c = f.drop(n+1)
        println(a + Console.REVERSED + Console.RED + b + Console.RESET + c)
        println((" " * a.size) + "^")
        None
      }
      case Right(ast) => Some(ast)
    }
}
