
# Skutek: extensible effects without (heavy) lifting

Skutek ([pronounced](https://translate.google.com/#pl/en/skutek): *skoo-tech*) is a framework implementing a monad of extensible effects 
(a.k.a. [*One monad to rule them all*](https://www.youtube.com/watch?v=KGJLeHhsZBo)), based on [Freer monad](http://okmij.org/ftp/Haskell/extensible/more.pdf), adapted to leverage specifics of Scala's type system, with main differences being:

- Use of intersection types in lieu of union types (which Scala doesn't have), to model sets of effects.

- Use of classic OOP inheritence as the way of extending the monad with new operations.

[![Build Status](https://travis-ci.org/marcinzh/skutek.svg?branch=master)](https://travis-ci.org/marcinzh/skutek)

# Example
```scala
import skutek.abstraction._
import skutek.std_effects._

object Main extends App {
  // Declare some effects:
  case object MyReader extends Reader[Int]
  case object MyState extends State[Int]
  case object MyExcept extends Except[String]

  // Create a monadic computation using those effects:
  val computation = for {
    a <- MyState.Get
    b <- MyReader.Ask
    c <- {
      if (b != 0) 
        Return(a / b)
      else 
        MyExcept.Raise(s"Tried to divide $a by zero")
    }
    _ <- MyState.Put(c)
  } yield ()

  // Create a handler for the above computation, by composing
  // individual handlers of each requested effect:
  val handler = MyExcept.handler <<<! MyState.handler(100).exec <<<! MyReader.handler(3)

  // Execute the computation using the handler:
  val result = handler.run(computation)

  println(result) // prints "Right(33)"
}
```

The inferred type of `computation` above is equivalent to:
```scala
  Unit !! MyState.type with MyReader.type with MyExcept.type
```
where `!!` is infix type alias for [Computation](./core/src/main/scala/skutek/Computation.scala) monad:
```scala
  Computation[Unit, MyState.type with MyReader.type with MyExcept.type]
```

---

More usage in [examples](./examples/src/main/scala/skutek_examples) directory.

# Setup

```scala
resolvers += Resolver.jcenterRepo

libraryDependencies += "com.github.marcinzh" %% "skutek-core" % "0.10.0"
```
Cross built for 2.11 and 2.12.

# Features

  **Warning:** contains links to partially outdated manual.

- Simplicity of use:
    - No need of defining the effect stack upfront. 
    - No need of lifting of operations into the monad.
    - Effect subtyping.
    
- Simplicity of implementation:
    - No dependencies on external libraries.
    - No clever type-fu, no macros. Mostly immutable OOP style.
     
- Practical stuff:
    - Predefined set of basic effects (`Reader`, `Writer`, etc.). [Read more](MANUAL.md#part-ii---predefined-effects).
    - Conflict proof: ability for multiple instances of the same type of effect, to coexist in the same effect stack (e.g. `State[Int]` and `State[String]`).
    - Potentially parallel execution of effects. [Read more](MANUAL.md#parallellism).
    - Support for `for` comprehension guards, for effects compatible with filtering (e.g. `Maybe`, `Choice`).
    - Tested stack safety.    
    
- Caveats and limitations:
    - General infancy of the project.
    - ~~No~~ Limited possiblity of adapting pre-existing monads as Skutek's effects.
    - Removing effects from the stack (local handling) isn't as easy as adding them. [Read more](MANUAL.md#62-local-handling).
    - Rare occurences of false positives by Scala's linter (i.e. "inferred `Any`"). [Read more](MANUAL.md#32-caveats).
    - Using patterns in `for` comprehensions can trigger surprising compiler errors. It can be mitigated by [This](https://github.com/oleg-py/better-monadic-for) compiler plugin.
    - Lack of performance analysis.
    - `Concurrency` effect is a [hack](MANUAL.md#warning).


# User Manual

  WIP. Partially outdated since many breaking chanches in 0.10.0

  [MANUAL.md](MANUAL.md)
