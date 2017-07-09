package skutek
import org.specs2._
import org.specs2.execute._, Typecheck._
import org.specs2.matcher.TypecheckMatchers._


class TypingTests extends Specification {

  def is = 
    InferredEffectful.is ^
    // HandlerAssociativity.is ^ 
    EffectfulSubtyping.is ^ 
    HandlerMatchingEffectful.is ^ 
    WithFilter.is ^
    LocalHandling.is


  object InferredEffectful {
    def is = br ^ "Effectful types" ! {
      val eff = for {
        a <- Get[Double]
        _ <- Tell("lies")
        c <- Choose(1 to 10)
        if c % 3 == 0
        b <- Ask[Boolean]
      } yield ()

      type Inferred = Unit !! Reader[Boolean] with State[Double] with Writer[String] with Choice

      typecheck {"implicitly[eff.type <:< Inferred]"} must succeed
    }
  }


/*
  // Doesn't pass on 2.11

  object HandlerAssociativity extends Dummies {
    def is = br ^ "Handler type composition should be associative" ! {
    
      type H12_3 = (H1 +! H2) +! H3
      type H1_23 = H1 +! (H2 +! H3)

      typecheck {"implicitly[H12_3 =:= H1_23]"} must succeed
    }
  }
*/

  object EffectfulSubtyping extends Dummies {
    def is = br ^ "Effect subtyping" ! (good and bad)
    def good = typecheck {"implicitly[Eff12 <:< Eff123]"} must succeed
    def bad  = typecheck {"implicitly[Eff12 <:< Eff1]"} must not succeed
  }


  object HandlerMatchingEffectful extends Dummies {
    type H12 = H1 +! H2
    type H21 = H2 +! H1

    def is = br ^ "Handler's effects should be superset of handled computation's effects" ! List(
      typecheck {"any[H12] run any[Eff1]"}   must succeed,
      typecheck {"any[H12] run any[Eff2]"}   must succeed,
      typecheck {"any[H12] run any[Eff12]"}  must succeed,
      typecheck {"any[H12] run any[Eff3]"}   must not succeed,
      typecheck {"any[H12] run any[Eff123]"} must not succeed,
      typecheck {"any[H21] run any[Eff1]"}   must succeed,
      typecheck {"any[H21] run any[Eff2]"}   must succeed,
      typecheck {"any[H21] run any[Eff12]"}  must succeed,
      typecheck {"any[H21] run any[Eff3]"}   must not succeed,
      typecheck {"any[H21] run any[Eff123]"} must not succeed,

      typecheck {"any[Eff1] runWith any[H12]"}   must succeed,
      typecheck {"any[Eff2] runWith any[H12]"}   must succeed,
      typecheck {"any[Eff12] runWith any[H12]"}  must succeed,
      typecheck {"any[Eff3] runWith any[H12]"}   must not succeed,
      typecheck {"any[Eff123] runWith any[H12]"} must not succeed,
      typecheck {"any[Eff1] runWith any[H21]"}   must succeed,
      typecheck {"any[Eff2] runWith any[H21]"}   must succeed,
      typecheck {"any[Eff12] runWith any[H21]"}  must succeed,
      typecheck {"any[Eff3] runWith any[H21]"}   must not succeed,
      typecheck {"any[Eff123] runWith any[H21]"} must not succeed
    ).reduce(_ and _)
  }


  object WithFilter {

    val eff1 = for {
      _ <- Ask[Double]
      _ <- Tell(true)
      _ <- Put("down")
    } yield ()
    
    val eff2 = Some(1).toEff

    def is = br ^ "Using guards in for comprehensions should be legal only after applying filterable effect" ! (bad1 and bad2 and good)

    def bad1 = typecheck {"""
      (for {
        _ <- eff1
        if true
      } yield ())
    """} must not succeed

    def bad2 = typecheck {"""
      (for {
        _ <- eff1
        if true
        _ <- eff2
      } yield ())
    """} must not succeed

    def good = typecheck {"""
      (for {
        _ <- eff1
        _ <- eff2
        if true
      } yield ())
    """} must succeed	
  }


  object LocalHandling extends Dummies {
    def is = br ^ "Local handling" ! (good111 and good21 and good3 and bad1 and bad2)

    def good111 = typecheck {"""
      any[Eff123]
      .handleCarefullyWith[Fx2 with Fx3](any[H1])
      .handleCarefullyWith[Fx3](any[H2])
      .handleCarefullyWith[Any](any[H3])
      .run
    """} must succeed	

    def good21 = typecheck {"""
      any[Eff123]
      .handleCarefullyWith[Fx3](any[H1 +! H2])
      .handleCarefullyWith[Any](any[H3])
      .run
    """} must succeed	

    def good3 = typecheck {"""
      any[Eff123]
      .handleCarefullyWith[Any](any[H1 +! H2 +! H3])
      .run
    """} must succeed	

    def bad1 = typecheck {"""
      any[Eff12]
      .handleCarefullyWith[Fx3](any[H1])
    """} must not succeed	

    def bad2 = typecheck {"""
      any[Eff12]
      .handleCarefullyWith[Fx1](any[H3])
    """} must not succeed	
  }
}
