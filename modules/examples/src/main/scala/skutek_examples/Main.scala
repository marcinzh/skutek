package skutek_examples

object Main extends App {

  val registry = List(
    "queens"  -> Queens.apply _,
    "invfuts" -> InvalidFutures.apply _,
    "sat"     -> sat_solver.Main.apply _
  )

  args.headOption.flatMap(arg => registry.toMap.get(arg.toLowerCase)) match { 
    case None => println("Available examples:\n" + registry.map(_._1).mkString("  ", "\n  ", ""))
    case Some(f) => f.apply(args.toIndexedSeq.drop(1))
  }
}
