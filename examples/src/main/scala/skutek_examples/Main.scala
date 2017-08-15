package skutek_examples

object Main extends App {

  val registry = List(
    "queens" -> Queens.apply _,
    "invalidfutures" -> InvalidFutures.apply _,
  )

  val args2 = if (args.isEmpty) args :+ registry.head._1 else args
  
  registry.toMap.get(args2.head.toLowerCase) match {
    case None => println("Available examples:\n" + registry.map(_._1).mkString("  ", "\n  ", ""))
    case Some(f) => f.apply
  }
}
