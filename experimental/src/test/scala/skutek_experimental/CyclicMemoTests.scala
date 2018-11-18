package skutek_experimental
import skutek._
import org.specs2._


class CyclicMemoTests extends Specification with CanLaunchTheMissiles {

  def is = graph

  def graph = br ^ "CyclicMemoizer operations should work" ! {

    val outgoings = Vector(
      List(0,1,2,3,4,5),
      List(6,7),
      List(7,2,1),
      List(3,7),
      List(),
      List(6),
      List(0),
      List()
    )

    val missiles = outgoings.map(_ => Missiles())

    case class Vertex(serno: Int, outgoing: List[Edge])
    case class Edge(from: () => Vertex, to: () => Vertex)

    def visit(n: Int) = {
      for {
        _ <- missiles(n).launch_!
        _ <- Tell(n)
        from <- CyclicMemo[Vertex](n)
        edges <- (
          for (i <- outgoings(n)) 
            yield for (to <- CyclicMemo[Vertex](i)) 
              yield Edge(from, to)
        ).serially
      } yield Vertex(n, edges)
    }

    val (roots, log) = 
      Vector(0)
      .map(CyclicMemo[Vertex](_)).serially
      .fx[Writer[Int]].handleWith(CyclicMemoizerHandler[Writer[Int]](visit))
      .flatten
      .runWith(WriterHandler.seq[Int])

    {
      def loop(todos: List[Vertex], visited: Set[Int]): Unit = {
        todos match {
          case Nil => ()
          case x :: rest => 
            val targets = x.outgoing.map(_.to())
            val more = targets.filterNot(v => visited.contains(v.serno))
            val visited2 = visited ++ more.map(_.serno)
            loop(rest ++ more, visited2)
        }
      }
      loop(roots.head() :: Nil, Set(roots.head().serno))
    }

    missiles.map(_.mustHaveLaunchedOnce).reduce(_ and _) and
    (log.sorted must_== (0 until outgoings.size))
  }
}
