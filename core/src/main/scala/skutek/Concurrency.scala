package skutek
import _internals._
import scala.concurrent._
import scala.concurrent.duration._


sealed trait Concurrency
object Concurrency extends NullaryEffectCompanion[Concurrency]

sealed class Run[A](val run: () => A) extends Operation[A, Concurrency]

object Run {
  def apply[A](run : => A) = new Run(() => run)
}

case class ConcurrencyHandler()(implicit ec: ExecutionContext) extends BaseHandlerWithSelfDriver[Concurrency] {
  type Secret[A, -U] = Future[A]
  type Result[A] = Future[A]
  type Op[A] = Run[A]

  def onReturn[A](a: A) = Future.successful(a)

  def onOperation[A, B, U](op: Op[A], k: A => Future[B]): Future[B] = Future { op.run() }.flatMap(k)

  def onProduct[A, B, C, U](aa: Future[A], bb: Future[B], k: ((A, B)) => Future[C]): Future[C] = (aa zip bb).flatMap(k)

  def onConceal[A, B, U, V](eff: A !! U, f: A => Future[B]): Future[B] = f(Interpreter.pure(eff))

  def onReveal[A, U](fut: Future[A]): Future[A] !! U = Return(fut)

  def await(timeout: Duration = Duration.Inf) = 
    new MappedHandler[Lambda[A => A]] {
      def apply[A](fut: Future[A]): A = Await.result(fut, timeout)
    }
}

object ConcurrencyHandler {
  def default = ConcurrencyHandler()(ExecutionContext.Implicits.global)
}
