# Skutek: extensible effects without (heavy) lifting

[![Build Status](https://travis-ci.org/marcinzh/skutek.svg?branch=master)](https://travis-ci.org/marcinzh/skutek)

Skutek ([pronounced](https://translate.google.com/#pl/en/skutek): *skoo-tech*) is a framework implementing a monad of extensible effects 
(a.k.a. [*One monad to rule them all*](https://www.youtube.com/watch?v=KGJLeHhsZBo)), based on [Freer monad](http://okmij.org/ftp/Haskell/extensible/more.pdf), adapted to leverage specifics of Scala's type system, with main differences being:

- Use of intersection types in lieu of union types (which Scala doesn't have), to model sets of effects.

- Use of classic OOP inheritence as the way of extending the monad with new operations.


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
    - No need of lifting of operations into the monad.
    - Effect subtyping.
    
- Simplicity of implementation:
    - No dependencies on external libraries.
    - No clever type-fu, no macros. Mostly immutable OOP style.
     
- Practical stuff:
    - Predefined set of basic effects (`Reader`, `Writer`, etc.).
    - Ability to annotate effects with tags
    - Potentially parallel execution of effects.
    - Support for `for` comprehension guards, for effects compatible with filtering (e.g. `Maybe`, `Choice`).
    - Tested stack safety.    
    
- Caveats and limitations:
    - General infancy of the project.
    - No possiblity of adapting pre-existing monads as Skutek's effects.
    - Removing effects from the stack (local handling) isn't as easy as adding them. Some explicit typing is necessary.
    - Rare occurences of false positives by Scala's linter (i.e. "inferred `Any`...")
    - Using patterns in `for` comprehensions can trigger surprising errors (Scala's wart, not specific to Skutek)
    - **Type unsafety:** Certain class of invalid effect stacks are detected **at runtime** only. 
    - Lack of performance analysis.
    - `Concurrency` effect is a hack.


# User Manual

In progress.‥

Meanwhile, see ~~[cheatsheet](./CHEATSHEET.md).~~

# Core concepts in Skutek

1. **Effect**
1. **Effect Stack**
1. **Computation** (the monad)
1. **Operation**
1. **Handler**

# 1\. Effect

In Skutek, an *Effect* is an abstract **type**, serving as a unique, type-level name. Such type is never instantiated or extended. *Effects* are only useful as type-arguments for other types or methods. Most notably, for types of *Computations* and *Handler* constructors.

Effects can be:
* parameterless, e.g. `Maybe`, `Choice`
* or parametrized, e.g. `State[Int]`, `Error[String]`

# 2\. Effect Stack

TBD.

1. Type `Any` represents **empty** *Effect Stack*

1. Type `Any` is the neutral element of type intersection operator. In Scala, the following types are equivalent:
    ```scala
    State[Int]
    State[Int] with Any
    Any with State[Int]
    ```
    Therefore, they all represent the same *Effect Stack*.

1. The order of occurence of *Effects* in the *Effect Stack* doesn't matter. In Scala, the following types are equivalent:
    ```scala
    State[Int] with Maybe
    Maybe with State[Int]
    ```
    Therefore, they all represent the same *Effect stack*.

1. Multiple occurences of the same *Effect* in the *Effect Stack*, are equivalent to just one occurence. In Scala, the following types are equivalent:
    ```scala
    State[Int]
    State[Int] with State[Int]
    ```
    Therefore, they all represent the same *Effect Stack*.

1. Bigger *Effect Stack* is a subtype of a smaller *Effect Stack*. For example:
    ```scala
    State[Int] with Maybe <: State[Int]
    State[Int] with Maybe <: Maybe
    ```
    This will have consequences for types of *Computations* and *Handlers* (contravariance).

Redundancies and reorderings shown in points 2, 3 and 4, may appear in error messages, when the *Effect Stack* of a *Computation* is inferred by Scala's compiler. It's ugly, but normal.

A curious reader may wonder, what happens if there are two occurences of the same, parametrised *Effect* in the *Effect Stack*, but each one is applied with different type-argument. For example:
```scala
State[Int] with State[String]
```
It will be discussed in the section about **Tag Conflicts**.

# 3\. Computation

### 3\.1\. Computation types

A *Computation* is value of a type derived from `Effectful[+A, -U]` trait.  
Parameter `A` is the result type of the *Computation*.  
Parameter `U` is the *Effect Stack* of the *Computation*.

Example:
```scala
type MyComputation = Effectful[Foo, State[Double] with Error[String] with Choice]
```
Same example, but using `!!`, an infix type alias for `Effectful`:
```scala
type MyComputation = Foo !! State[Double] with Error[String] with Choice
```

The latter is more readable, as long as you remember that:
* Precedence of `!!` is lower than of `with`, so: `A !! U with V` == `A !! (U with V)`
* Precedence of `!!` is higher than of `=>`, so: `A => B !! U` == `A => (B !! U)`

### 3\.2\. Computation values

##### 3\.2\.1\. Return
The simplest *Computation* is constructed by `Return(x)` case class, where `x` is any value.  

`Return(_)` is similar to `Pure(_)`, `Some(_)`, `Right(_)`, `Success(_)` in other monads. Except in Skutek, `Return(_)` is shared for all possible *Effects*.

This *Computation* has empty *Effect Stack*:
```scala
// assuming: 
val a : A = ???

// let:
val eff = Return(a)

// we get:
eff : A !! Any
```

Also, `Return()` is an abbreviation of `Return(())`.

##### 3\.2\.2\. Composing Computations

*Computation* is a monad, so standard `map`, `flatMap` and `flatten` methods are available.

When 2 *Computations* are composed using `flatMap`, their *Effect Stacks* are automatically merged, by type intersection:
```scala
// assuming:
val eff1 : A !! U1 = ???
val eff2 : B !! U2 = ???

// let:
val eff3 = eff1.flatMap(_ => eff2)

// we get:
eff3 : B !! U1 with U2 
```

*Computations* can also be composed parallely, using product operator: `*!`

The parallellism is potential only: it may or may not be actually happening, depending on *Handler* used to execute the *Computation*.

Just like in case of `flatMap`, the *Effect Stack* of product equals *Effect Stack* is are automatically merged, by type intersection:



# 4\. Operation

TBD.

An *Operation* is an elementary *Computation*, specific for an *Effect*.  
*Operations* are defined as dumb case classes, indirectly inheriting from `Effectful` trait.

Examples:

|Constructor of *Operation* | *Effect* of the *Operation* | Type of the *Computation*|
|---|---|---|
|`Get[Double]`        |`State[Double]`   | `Double !! State[Double]`| 
|`Put(1.337)`         | same as above    | `Unit !! State[Double]`| 
|`Tell("Hello")`      |`Writer[String]`  | `Unit !! Writer[String]`|
|`Choose(1 to 10)`    |`Choice`          | `Int !! Choice`|

Nullary *Operation* require explicit type parameter, like in the case of `Get[Double]`.

# 5\. Handler

*Handler* is an object, that has ability to handle an *Effect* (or *Effects*). 

Handling an *Effect* (or *Effects*), is an act of removing some *Effect* (or *Effects*) from 
the *Computation's* *Effect Stack*. Possibly, also transforming *Computation's* result 
type in the process.

Handling *Effects* is also the point, where **the order of effects** starts to matter.

After all *Effects* are handled, *Computation's* *Effect Stack* is empty (i.e. provable to be `=:= Any`).
Then, the *Computation* is ready to be executed. Obtained value is finally liberated from the monadic context:
```scala
// assuming:
eff : A !! Any

// let:
val a = eff.run   

// we get:
a : A
```
### 5\.1\. Elementary handlers
Every effect definiton provides a handler for its own *Effect*. Examples:

| Constructor of *Handler* | *Effect* it handles | How the *Handler* transforms </br> *Computation's* result type `A` |
|---|---|---|
|`StateHandler(42.0)`|`State[Double]`| `(A, Double)` |
|`ErrorHandler[String]`|`Error[String]`|`Either[String, A]`|
|`ChoiceHandler`|`Choice`|`Vector[A]`|

### 5\.2\. Composing handlers
Multiple *Handlers* can be associatively composed using `+!` operator, forming *Handler*
that can handle bigger sets of *Effects*. 

For example, *Handler*:
```scala
val handler = StateHandler(42.0) +! ErrorHandler[String] +! ChoiceHandler
```

Can handle all the *Effects* in the following *Effects Stack*:

```scala
State[Double] with Error[String] with Choice
```

The order of composition matters. *Handlers* are applied from right to left.

### 5\.3\. Full handling

The **easiest** way of using *Handlers*, is to handle all *Effects* at once: 
1. Create composed *Handler*, covering all *Effects* in the *Computation's* *Effect Stack*.
2. Handle *Effects* and execute the *Computation*, both in one call.

Example:
```scala
// assuming:
eff : Int !! State[Double] with Choice = ???

// Step 1.
val handler = StateHandler(1.377) +! ChoiceHandler

// Step 2.
val result = handler.run(eff) 

val result = eff.runWith(handler)   // alternative syntax

// we get:
result : (Vector[Int], Double)
```
TBD.

# Traversing

Traversing is a transformation of a collection-of-*Computations* into a *Computation*-of-collection.

TBD.

# Tag Conflicts

TBD.
