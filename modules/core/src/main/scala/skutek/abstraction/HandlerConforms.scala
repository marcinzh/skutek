package skutek.abstraction
import scala.annotation.implicitNotFound

@implicitNotFound(msg =
  "Can't apply the handler to the computation, because their effects are incompatible."+
  "\nIt is required, that union of:"+
  "\n 1) set of effects handled by the handler (baked in the handler's type)"+
  "\n 2) set of effects passed over by the handler (specified by caller, in explicit type parameter of handle[U] method)"+
  "\nis equal to, or greater than set of effects requested by the computation."+
  "\nIn essence, the caller must manually compute the difference of sets. Type checker can only verify the result."+
  "\nSet of effects requested by the computation: ${V}"+
  "\nSet of effects passed over by the handler:   ${U}")
//// U := V \ W
sealed trait HandlerConforms[U, V, W] {
  def apply[A](ma: A !! V): A !! W with U
}

object HandlerConforms {
  private[abstraction] val singleton = new HandlerConforms[Any, Any, Any] {
    def apply[A](ma: A !! Any): A !! Any = ma
  }
}

trait HandlerConforms_exports {
  implicit def HandlerConforms_evidence[U, V, W](implicit ev: W with U <:< V): HandlerConforms[U, V, W] =
    HandlerConforms.singleton.asInstanceOf[HandlerConforms[U, V, W]]
}
