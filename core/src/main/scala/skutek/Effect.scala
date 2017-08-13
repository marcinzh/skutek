package skutek
import scala.reflect.ClassTag


sealed trait @![-Fx, Tag]

trait FilterableEffect

private[skutek] final class TagOfFx[Fx](val tag: Any) 

trait Effect_exports {
  implicit def explicitTagOfFx[Fx, Tag](implicit ev: ClassTag[Tag]) = new TagOfFx[@![Fx, Tag]](ev)
}
