package skutek
package _internals
import scala.reflect.ClassTag


abstract class NullaryEffectCompanion[Fx](implicit ev: ClassTag[Fx]) {
  implicit def implicitlyTagged = new TagOfFx[Fx](ev)
}

abstract class UnaryEffectCompanion[Fx[_]](implicit ev: ClassTag[Fx[_]]) {
  implicit def implicitlyTagged[T] = new TagOfFx[Fx[T]](ev)
}
