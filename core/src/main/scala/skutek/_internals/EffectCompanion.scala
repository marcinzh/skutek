package skutek
package _internals
import scala.reflect.ClassTag


abstract class NullaryEffectCompanion[Fx](implicit ev: ClassTag[Fx]) {
  implicit def implicitTagOfFx = new TagOfFx[Fx](ev)
}

abstract class UnaryEffectCompanion[Fx[_]](implicit ev: ClassTag[Fx[_]]) {
  implicit def implicitTagOfFx[T] = new TagOfFx[Fx[T]](ev)
}
