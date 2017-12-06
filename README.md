
# Skutek: extensible effects without (heavy) lifting

Skutek ([pronounced](https://translate.google.com/#pl/en/skutek): *skoo-tech*) is a framework implementing a monad of extensible effects 
(a.k.a. [*One monad to rule them all*](https://www.youtube.com/watch?v=KGJLeHhsZBo)), based on [Freer monad](http://okmij.org/ftp/Haskell/extensible/more.pdf), adapted to leverage specifics of Scala's type system, with main differences being:

- Use of intersection types in lieu of union types (which Scala doesn't have), to model sets of effects.

- Use of classic OOP inheritence as the way of extending the monad with new operations.

[![Build Status](https://travis-ci.org/marcinzh/skutek.svg?branch=master)](https://travis-ci.org/marcinzh/skutek)

# Example
```scala
import skutek._

object Main extends App {

  val computation = for {
    a <- Get[Int]
    b <- Ask[Int]
    c <- if (b != 0) Return(a / b) else Wrong(s"Tried to divide $a by zero")
    _ <- Put(c)
  } yield ()

  val handler = ErrorHandler[String] +! StateHandler(100).exec +! ReaderHandler(3)

  val result = handler.run(computation)

  println(result) // prints "Right(33)"
}
```

The inferred type of `computation` above is equivalent to:
```scala
  Unit !! State[Int] with Reader[Int] with Error[String]
```

More in [examples](https://github.com/marcinzh/skutek/tree/master/examples/src/main/scala/skutek_examples) directory.

# Setup

```scala
resolvers += Resolver.jcenterRepo

libraryDependencies += "com.github.marcinzh" %% "skutek-core" % "0.6.0"
```
Cross built for 2.11 and 2.12.

# Features

- Simplicity of use:
    - No need of defining the effect stack upfront. 
    - No need of lifting of operations into the monad.
    - Effect subtyping.
    
- Simplicity of implementation:
    - No dependencies on external libraries.
    - No clever type-fu, no macros. Mostly immutable OOP style.
     
- Practical stuff:
    - Predefined set of basic effects (`Reader`, `Writer`, etc.). [Read more](MANUAL.md#part-ii---predefined-effects).
    - Ability to annotate effects with tags. [Read more](MANUAL.md#tagging-effects).
    - Potentially parallel execution of effects. [Read more](MANUAL.md#parallellism).
    - Support for `for` comprehension guards, for effects compatible with filtering (e.g. `Maybe`, `Choice`).
    - Tested stack safety.    
    
- Caveats and limitations:
    - General infancy of the project.
    - No possiblity of adapting pre-existing monads as Skutek's effects.
    - Removing effects from the stack (local handling) isn't as easy as adding them. [Read more](MANUAL.md#62-local-handling).
    - Rare occurences of false positives by Scala's linter (i.e. "inferred `Any`"). [Read more](MANUAL.md#32-caveats).
    - Using patterns in `for` comprehensions can trigger surprising errors (Scala's wart, not specific to Skutek).
    - **Type unsafety:** Certain class of invalid effect stacks are detected **at runtime** only. [Read more](MANUAL.md#tag-conflicts).
    - Lack of performance analysis.
    - `Concurrency` effect is a [hack](MANUAL.md#warning).


# User Manual

  [MANUAL.md](MANUAL.md)
  
