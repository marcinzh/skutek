package skutek
import _internals._
import scala.concurrent._
import scala.concurrent.duration._


sealed trait Concurrency
object Concurrency extends EffectCompanion0[Concurrency]

sealed trait ConcurrencyOperation[A] extends Operation[A, Concurrency]
sealed class Run[A](val run: () => A) extends ConcurrencyOperation[A]
case class FutureWrapper[A](future: Future[A]) extends ConcurrencyOperation[A]

object Run {
  def apply[A](run : => A) = new Run(() => run)
}

object RunEff {
  def apply[A, U](run : => A !! U) = new Run(() => run).flatten
}


case class ConcurrencyHandler()(implicit ec: ExecutionContext) extends UniversalHandler[Concurrency] {
  type Result[A] = Future[A]
  type Secret[A, -U] = Future[A]
  type Op[A] = ConcurrencyOperation[A]

  def onReturn[A](a: A): Secret[A, Any] = 
    Future.successful(a)

  def onOperation[A, B, U](op: Op[A]): Cont[A, B, U] = 
    k => (op match {
      case r: Run[A] => Future { r.run() }
      case FutureWrapper(fut) => fut
    }).flatMap(k)

  def onProduct[A, B, C, U](aa: Secret[A, U], bb: Secret[B, U]): Cont[(A, B), C, U] = 
    k => (aa zip bb).flatMap(k)

  def onConceal[A, B, U](a_! : A !! U): Cont[A, B, U] =
    k => k(Interpreter.pure(a_!))

  def onReveal[A, U](aa: Secret[A, U]): Result[A] !! U =
    Return(aa)

  def await(timeout: Duration = Duration.Inf) = 
    new MappedHandler[Lambda[A => A]] {
      def apply[A](fut: Future[A]): A = Await.result(fut, timeout)
    }
}

object ConcurrencyHandler {
  def global = ConcurrencyHandler()(ExecutionContext.Implicits.global)
}

trait Concurrency_exports {
  implicit class FutureToComputation[A](thiz: Future[A]) {
    def toEff: A !! Concurrency = FutureWrapper(thiz)
    def toEff[Tag](tag: Tag): A !! (Concurrency @! Tag) = FutureWrapper(thiz) @! tag
  }
}
