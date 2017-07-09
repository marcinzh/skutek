package skutek
import _internals.{Interpreter, Driver}
import scala.reflect.ClassTag


sealed trait Handler { outer =>
  type Effects
  type Result[X]

  def interpret[A, U](eff: A !! U with Effects): Result[A] !! U
  final def handleCarefully[U] = new HandlePoly[U]
  final def run[A](eff: A !! Effects): Result[A] = interpret(eff).run
  val tags: Set[Any]

  final class HandlePoly[U] {
    def apply[A](eff: A !! U with Effects): Result[A] !! U = interpret[A, U](eff)
  }

  final def fx[Fx](implicit ev: TagOfFx[Fx]) = new TagSetBuilder[Any](Set[Any]()).fx[Fx]

  final class TagSetBuilder[U](accum: Set[Any]) {
    def fx[Fx](implicit ev: TagOfFx[Fx]): TagSetBuilder[U with Fx]= {
      val tag = ev.tag
      if (accum.contains(tag) || outer.tags.contains(tag))
        sys.error(s"Effect tag conflict detected: $tag")
      else
        new TagSetBuilder[U with Fx](accum + tag)
    }

    def handle = new HandlePoly[U]
  }
}


protected final case class ComposedHandler[H1 <: Handler, H2 <: Handler](lhs: H1, rhs: H2) extends Handler {
  type Effects = H1#Effects with H2#Effects
  type Result[X] = H1#Result[H2#Result[X]]
  val tags = lhs.tags | rhs.tags

  if (tags.size != lhs.tags.size + rhs.tags.size) {
    val badTags = (lhs.tags & rhs.tags).toVector.map(_.toString).sorted.mkString(", ")
    sys.error(s"Effect tag conflict detected: $badTags")
  }

  def interpret[A, U](eff: A !! U with Effects): Result[A] !! U = {
    val eff2 = rhs.interpret[A, U with H1#Effects](eff)
    lhs.interpret[H2#Result[A], U](eff2)
  }
}


protected sealed trait ElementalHandler extends Handler { outer =>
  def myTag: Any
  final val tags: Set[Any] = Set(myTag)
 	def interpretWithTag[A, U](eff: A !! U with Effects, tag: Any): Result[A] !! U
  final def interpret[A, U](eff: A !! U with Effects) = interpretWithTag[A, U](eff, myTag)
}


protected sealed trait TaggableHandler extends ElementalHandler { outer =>

  abstract class MappedHandler[S[_]] extends TaggableHandler {
    final type Effects = outer.Effects
    final type Result[A] = S[A]
    final def myTag: Any = outer.myTag
   	final def interpretWithTag[A, U](eff: A !! U with Effects, tag: Any): S[A] !! U = 
   		outer.interpretWithTag[A, U](eff, tag).map(x => apply(x))

    def apply[A](x: outer.Result[A]): S[A]
  }

  final def @![Tag](explicitTag: Tag) = new ElementalHandler {
    type Effects = outer.Effects @! Tag
    type Result[X] = outer.Result[X]
    def myTag: Any = explicitTag
 		def interpretWithTag[A, U](eff: A !! U with Effects, tag: Any): Result[A] !! U =
      outer.interpretWithTag[A, U](eff.sideCast[U with outer.Effects], tag)
  }
}


abstract class BaseHandler[Fx](implicit implicitTag: ClassTag[Fx]) extends TaggableHandler { 
  final type Effects = Fx
  final def myTag: Any = implicitTag
}


abstract class BaseHandlerWithDriver[Fx](implicit implicitTag: ClassTag[Fx]) extends BaseHandler { outer =>

 	type Drv = Driver { 
 		type Result[X] = outer.Result[X]
 		type Effects = Fx
 	}

 	def driver: Drv  

 	final def interpretWithTag[A, U](eff: A !! U with Effects, tag: Any): Result[A] !! U = 
    Interpreter.impure[A, U, Effects, Drv](driver, tag, eff)
}


abstract class BaseHandlerWithSelfDriver[Fx](implicit implicitTag: ClassTag[Fx]) extends BaseHandlerWithDriver with Driver { 
  final def driver = this
}


protected trait Handler_exports {
  type +![H1 <: Handler, H2 <: Handler] = Handler { 
    type Effects = H1#Effects with H2#Effects
    type Result[X] = H1#Result[H2#Result[X]]
  }

  implicit class Handler_extension[H1 <: Handler](thiz: H1) {
    def +![H2 <: Handler](that: H2): H1 +! H2 = ComposedHandler(thiz, that)
  }

  implicit class EffectfulRun_extension[A, U](thiz: A !! U) {
    def run(implicit ev: U =:= Any): A = Interpreter.pure(thiz)
    def runWith(h: Handler { type Effects <: U }) = h.run(thiz)
    def handleCarefullyWith[V] = new HandleWithPoly[V]

    final class HandleWithPoly[V] {
      def apply[H <: Handler](h: H)(implicit ev: (A !! U) <:< (A !! H#Effects with V)) = h.interpret[A, V](thiz)
    }		

    def fx[Fx](implicit ev: TagOfFx[Fx]) = new TagSetBuilder[Any](Set[Any]()).fx[Fx]

    final class TagSetBuilder[V](accum: Set[Any]) {
      def fx[Fx](implicit ev: TagOfFx[Fx]): TagSetBuilder[V with Fx]= {
        val tag = ev.tag
        if (accum.contains(tag))
          sys.error(s"Effect tag conflict detected: $tag")
        else
          new TagSetBuilder[V with Fx](accum + tag)
      }

      def handleWith[H <: Handler](h: H)(implicit ev: (A !! U) <:< (A !! h.Effects with V)) = {
        if ((accum & h.tags).size == 0) 
          h.interpret[A, V](thiz)
        else {
          val badTags = (accum & h.tags).toVector.map(_.toString).sorted.mkString(", ")
          sys.error(s"Effect tag conflict detected: $badTags")
        }
      }
    }
  }
}
