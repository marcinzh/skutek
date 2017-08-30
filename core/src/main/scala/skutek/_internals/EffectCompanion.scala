package skutek
package _internals
import scala.reflect.ClassTag


abstract class EffectCompanion0[Fx](implicit ev: ClassTag[Fx]) {
  implicit def implicitTagOfFx = new TagOfFx[Fx](ev)
}

abstract class EffectCompanion1[Fx[_]](implicit ev: ClassTag[Fx[_]]) {
  implicit def implicitTagOfFx[A] = new TagOfFx[Fx[A]](ev)
}

abstract class EffectCompanion2[Fx[_, _]](implicit ev: ClassTag[Fx[_, _]]) {
  implicit def implicitTagOfFx[A, B] = new TagOfFx[Fx[A, B]](ev)
}

abstract class EffectCompanion3[Fx[_, _, _]](implicit ev: ClassTag[Fx[_, _, _]]) {
  implicit def implicitTagOfFx[A, B, C] = new TagOfFx[Fx[A, B, C]](ev)
}

abstract class EffectCompanion4[Fx[_, _, _, _]](implicit ev: ClassTag[Fx[_, _, _, _]]) {
  implicit def implicitTagOfFx[A, B, C, D] = new TagOfFx[Fx[A, B, C, D]](ev)
}

abstract class EffectCompanion5[Fx[_, _, _, _, _]](implicit ev: ClassTag[Fx[_, _, _, _, _]]) {
  implicit def implicitTagOfFx[A, B, C, D, E] = new TagOfFx[Fx[A, B, C, D, E]](ev)
}
