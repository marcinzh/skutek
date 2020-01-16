package skutek_experimental
import skutek.abstraction._
import skutek.std_effects._
import org.specs2._


class AcyclicMemoTest extends Specification with CanLaunchTheMissiles {

  def is = graph

  def graph = br ^ "AcyclicMemoizer operations should work" ! {

    case object FxMemo extends AcyclicMemoizer[Int, Vertex]
    case object FxW extends Writer[Vector[Int]]

    case class Vertex(serno: Int, outgoing: List[Edge])
    case class Edge(to: Vertex)

    val outgoings = Vector(
      /*0*/ List(1,2,3,4,5),
      /*1*/ List(2,6,7),
      /*2*/ List(5,6),
      /*3*/ List(6,7),
      /*4*/ List(),
      /*5*/ List(6),
      /*6*/ List(),
      /*7*/ List()
    )

    val missiles = outgoings.map(_ => Missile())


    def visit(n: Int) = {
      for {
        _ <- missiles(n).launch_!
        _ <- FxW.Tell(n)
        edges <- (
          for (i <- outgoings(n))
            yield for (to <- FxMemo.Recur(i))
              yield Edge(to)
        ).traverse
      } yield Vertex(n, edges)
    }

    val (log, roots) =
      Vector(0)
      .map(FxMemo.Recur(_)).traverse
      .handleWith[FxW.type](FxMemo.handler[FxW.type](visit))
      .runWith(FxW.handler)

    {
      def loop(todos: List[Vertex], visited: Set[Int]): Unit = {
        todos match {
          case Nil => ()
          case x :: rest =>
            val targets = x.outgoing.map(_.to)
            val more = targets.filterNot(v => visited.contains(v.serno))
            val visited2 = visited ++ more.map(_.serno)
            loop(rest ++ more, visited2)
        }
      }
      loop(roots.head :: Nil, Set(roots.head.serno))
    }

    missiles.map(_.mustHaveLaunchedOnce).reduce(_ and _) and
    (log.sorted must_== (0 until outgoings.size))
  }
}
