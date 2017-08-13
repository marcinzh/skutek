package skutek
package _internals


trait SyntheticTagger {
  type Tagged[Fx]
  def apply[A, Fx](op: Operation[A, Fx]): A !! Tagged[Fx]
}

object SyntheticTagger {

  case class Explicit[Tag](tag: Tag) extends SyntheticTagger {
    type Tagged[Fx] = Fx @! Tag
    def apply[A, Fx](op: Operation[A, Fx]): A !! Tagged[Fx] = op @! tag
  }

  object Implicit extends SyntheticTagger {
    type Tagged[Fx] = Fx
    def apply[A, Fx](op: Operation[A, Fx]): A !! Tagged[Fx] = op
  }
}
