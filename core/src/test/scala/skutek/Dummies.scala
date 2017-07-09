package skutek


trait Dummies {
  type H1 = ReaderHandler[Boolean]
  type H2 = WriterHandler[String] 
  type H3 = StateHandler[Float]  
  type Fx1 = Reader[Boolean]
  type Fx2 = Writer[String]
  type Fx3 = State[Float]

  type Eff1 = Whatever !! Fx1
  type Eff2 = Whatever !! Fx2 
  type Eff3 = Whatever !! Fx3
  type Eff12 = Whatever !! Fx1 with Fx2 
  type Eff23 = Whatever !! Fx2 with Fx3
  type Eff123 = Whatever !! Fx1 with Fx2 with Fx3

  class Whatever
  def any[T] : T = ???
}
