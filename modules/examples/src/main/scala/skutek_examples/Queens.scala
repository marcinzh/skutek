package skutek_examples
import skutek.abstraction._
import skutek.std_effects._


object Queens {
  case object FxS extends State[Solution]
  case object FxC extends Choice


  def apply(args: Seq[String]) = {
    val boardSize = 8
    search(boardSize)
    .runWith(FxC.findFirst)
    .foreach { solution => 
      printSolution(boardSize, solution)
    }
  }
  

  type Solution = Vector[Placement]

  case class Placement(column: Int, row: Int) {
    def collidesWith(that: Placement) = {
      val x = column - that.column
      val y = row - that.row
      x == 0 || y == 0 || x == y || x == -y
    }
  }

  def search(boardSize: Int): Solution !! FxC.type = {
    (for (row <- 0 until boardSize) yield 
      for {
        solution <- FxS.Get
        column <- FxC.Choose(0 until boardSize)
        p = Placement(column, row)
        if !solution.exists(_.collidesWith(p))
        _ <- FxS.Put(solution :+ p)
      } yield ()
    )
    .traverseVoid
    .handleWith[FxC.type](FxS.handler(Vector.empty[Placement]).justState)
  }


  def printSolution(boardSize: Int, ps: Solution) = {
    def u(s: String) = Console.UNDERLINED + s + Console.RESET
    println(Vector.fill(boardSize)(u(" ")).mkString("  ", " ", ""))
    for (p <- ps) {
      val line = Vector.fill(boardSize)(u(" ")).updated(p.column, u("\u265B")).mkString("|", "|", "|")
      println(s"${boardSize - p.row}${line}")
    }
    println((0 until boardSize).map(i => ('a'.toInt + i).toChar).mkString("  ", " ", " "))
  }
}
