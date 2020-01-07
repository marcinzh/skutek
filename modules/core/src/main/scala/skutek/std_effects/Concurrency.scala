package skutek.std_effects
import skutek.abstraction._
import skutek.abstraction.custom_effect._
import scala.concurrent._
import scala.concurrent.duration._


case object Concurrency extends Concurrency

trait Concurrency extends EffectImpl {
  sealed class Run[A](val run: () => A) extends Op[A]
  case class FutureWrapper[A](future: Future[A]) extends Op[A]

  object Run {
    def apply[A](run : => A) = new Run(() => run)
  }

  object RunEff {
    def apply[A, U](run : => A !! U) = new Run(() => run).flatten
  }

  class CommonHandler(implicit ec: ExecutionContext) extends Ultimate with Parallel {
    final override type Result[A] = Future[A]

    final override def onReturn[A, U](a: A): A !@! U =
      Future.successful(a)

    final override def onOperation[A, B, U](op: Op[A], k: A => B !@! U): B !@! U =
      (op match {
        case r: Run[A] => Future { r.run() }
        case FutureWrapper(fut) => fut
      }).flatMap(k)

    final override def onProduct[A, B, C, U](ma: A !@! U, mb: B !@! U, k: ((A, B)) => C !@! U): C !@! U =
      (ma zip mb).flatMap(k)

    final def await(timeout: Duration = Duration.Inf) =
      map[Lambda[A => A]](new Into[Lambda[A => A]] {
        def apply[A](fut: Future[A]): A = Await.result(fut, timeout)
      })
  }

  def handler(implicit ec: ExecutionContext) = new CommonHandler
  def global = handler(ExecutionContext.Implicits.global)

  // def await(timeout: Duration = Duration.Inf)(implicit ec: ExecutionContext) = handler.await(timeout)
}
