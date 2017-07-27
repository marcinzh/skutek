# Skutek: extensible effects without (heavy) lifting

TBD

# Example
```scala
import skutek._

object Main extends App {

  val eff = for {
    a <- Get[Int]
    b <- Ask[Int]
    c <- if (b != 0) Return(a / b) else Wrong(s"Tried to divide $a by zero")
    _ <- Put(c)
  } yield ()

  val handler = ErrorHandler[String] +! StateHandler(100).exec +! ReaderHandler(3)

  val result = handler.run(eff)

  println(result) // prints "Right(33)"
}
```
More in [examples](https://github.com/marcinzh/skutek/tree/master/examples/src/main/scala/skutek_examples) directory.

# Setup

```scala
resolvers += Resolver.jcenterRepo

libraryDependencies += "com.github.marcinzh" %% "skutek-core" % "0.4.0"
```

# Features

- Simplicity of use:
    - No need of defining the effect stack upfront. 
    - No need of "lifting into the monad".
    - Effect extensiblity is provided by classic OOP inheritance.
    - Effect subtyping.
    
- Simplicity of implementation:
    - No dependencies on external libraries.
    - No clever type-fu, no macros. Mostly immutable OOP style.
     
- Practical stuff:
    - Predefined set of basic effects (`Reader`, `Writer`, etc.).
    - Ability to annotate effects with tags
    - Potentially parallel execution of effects, without breaking monad laws.
    - Support for `for` comprehension guards, for effects compatible with filtering (e.g. `Maybe`, `Choice`).
    - Tested stack safety.    
    
- Caveats and limitations:
    - General infancy of the project.
    - No possiblity of adopting pre-existing monads as Skutek's effects.
    - Removing effects from the stack (local handling) isn't as easy as adding them. Some explicit typing is necessary.
    - Rare occurences of false positives by Scala's linter (i.e. "inferred `Any`...")
    - Using patterns in `for` comprehensions can trigger surprising errors (Scala's wart, not specific to Skutek)
    - **Type unsafety:** Certain class of invalid effect stacks are detected **at runtime** only. 
    - Lack of performance analysis.
    - `Concurrency` effect is a hack.


#User Manual

In progress.‥

Meanwhile, see [cheatsheet](CHEATSHEET.md).


