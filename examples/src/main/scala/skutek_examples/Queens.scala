package skutek_examples
import skutek._


object Queens {

  def apply(args: Seq[String]) = {
    val boardSize = 8
    search(boardSize)
    .runWith(ChoiceHandler.FindFirst)
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


  def search(boardSize: Int): Solution !! Choice = {
    (for (row <- 0 until boardSize) yield 
      for {
        solution <- Get[Solution]
        column <- Choose(0 until boardSize)
        p = Placement(column, row)
        if !solution.exists(_.collidesWith(p))
        _ <- Put(solution :+ p)
      } yield ()
    )
    .seriallyVoid
    .handleCarefullyWith(StateHandler(Vector.empty[Placement]).exec)
  }


  def printSolution(boardSize: Int, ps: Solution) = {
    def u(s: String) = Console.UNDERLINED + s + Console.RESET
    println(Vector.fill(boardSize)(u(" ")).mkString("  ", " ", ""))
    for (p <- ps) {
      val line = Vector.fill(boardSize)(u(" ")).updated(p.column, u("\u265B")).mkString("|", "|", "|")
      println((boardSize - p.row) + line)
    }
    println((0 until boardSize).map(i => ('a'.toInt + i).toChar).mkString("  ", " ", " "))
  }
}
