package skutek


object Trampoline {
  def apply[A, U](block : => A !! U): A !! U = 
    for { 
      _ <- Return()
      a <- block 
    } yield a
}


object Eval {
  def apply[A](block : => A): A !! Any = 
    for { 
      _ <- Return() 
    } yield block
}
