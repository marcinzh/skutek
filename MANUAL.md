
# Table of Contents

### Part I: Core concepts in Skutek
* [1. **Effect Definition**](#1-effect-definition)
* [2. **Effect**](#2-effect)
* [3. **Effect Stack**](#3-effect-stack)
  * [3.1. Properties](#31-properties)
  * [3.2. Caveats](#32-caveats)
* [4. **Computation**](#4-computation)
  * [4.1. Return](#41-return)
  * [4.2. Operations](#42-operations)
  * [4.3. Composing Computations](#43-composing-computations)
* [5. **Handler**](#5-handler)
  * [5.1. Elementary Handlers](#51-elementary-handlers)
  * [5.2. Composed Handlers](#52-composed-handlers)
* [6. **Handling Effects**](#6-handling-effects)
  * [6.1. Full handling](#61-full-handling)
  * [6.2. Local handling](#62-local-handling)
    * [6.2.1. The simpler way](#621-the-simpler-way)
    * [6.2.2. The safer way](#622-the-safer-way)
### Part II: Predefined Effects
- [Reader Effect](#reader-effect)
- [Writer Effect](#writer-effect)
- [State Effect](#state-effect)
- [Maybe Effect](#maybe-effect)
- [Error Effect](#error-effect)
- [Validation Effect](#validation-effect)
- [Choice Effect](#choice-effect)
- [Concurrency Effect](#concurrency-effect)
- [Eval & Trampoline](#eval--trampoline)
### Part III: Advanced Topics
- [Traversing](#traversing)
- [Parallellism](#parallellism)
- [Tagging Effects](#tagging-effects)
- [Synthetic Operations](#synthetic-operations)
- [Tag Conflicts](#tag-conflicts)
- [Mapped Handlers](#mapped-handlers)
- [Defining your own Effect](#defining-your-own-effect)

# Part I: Core concepts in Skutek

## 1\. Effect Definition

*Effect Definition*, is a fragment of program, that **extends** functionality of `Effectful` monad. The monad, which is all that Skutek is about.

An *Effect Definition* contains definitions of 3 kinds of entities:
* An *Effect*.
* *Operation(s)* of that *Effect*.
* *Handler(s)* of that *Effect*.

Skutek comes with [§. predefined](#part-ii---predefined-effects) effects. User can define [§. new effects](#defining-your-own-effect) as well.


## 2\. Effect

In Skutek, an *Effect* is an **abstract type** (a trait), serving as a unique, type-level **name**. Such type is supposed to be never instantiated or inherited. *Effects* are only useful as type-arguments for other types or methods. Most notably, for types of *Computations* and *Handler* constructors.

*Effects* can be:
* parameterless, e.g. `Maybe`, `Choice`
* or parametrized, e.g. `State[Int]`, `Error[String]`

## 3\. Effect Stack

An *Effect Stack* in Skutek, is a type-level **set** of *Effects*.  

It's a misnomer to call it a "stack", as it wrongfully (in case of Skutek) suggests significance of the order of elements. Despite that, we're going to use this term, for traditon and convenience.

Skutek uses **intersection types** as the representation of *Effect Stacks*.

For example:
```scala
State[Double] with Error[String] with Choice
```
represents 3-element *Effect Stack*, comprising 3 *Effects*: `State[Double]`, `Error[String]` and `Choice`.

### 3\.1\. Properties

The nature of intersection types gives raise to the following properties of *Effect Stacks*:

1. An *Effect* is also a single element *Effect Stack*.

1. Type `Any` represents **empty** *Effect Stack*.

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
    Therefore, they all represent the same *Effect Stack*.

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
    This has useful consequences for types of *Computations* and *Handlers* - effect subtyping:
    * *Computation* with bigger *Effect Stack* can be substituted by a *Computation* with smaller *Effect Stack*.
    * *Handler* of smaller *Effect Stack* can be substituted by a *Handler* of bigger *Effect Stack*.
    
    Clarification: when mentioning "bigger" and "smaller" *Effect Stacks*, we refer to pair of sets of *Effect*, in subset/superset relation.

### 3\.2\. Caveats

There are caveats related to intersection types:
* Redundancies and reorderings shown in points 3, 4 and 5, may appear in error messages, when the *Effect Stack* of a *Computation* is inferred by Scala's compiler. It's ugly, but normal.

* Occasionally, when using empty *Effect Stacks*, Scala compiler's linter may complain with message:
  ```
  a type was inferred to be `Any`, which usually indicates programming error
  ```
  This is a false positive. The solution is to either:
  - Add explicit types here and there, until the linter shuts up.
  - Selectively disable this specific feature of linter (and if it works for you, let me know).
  - Disable the linter entirely.
  
* A curious reader may wonder, what happens if there are two occurences of the same, parametrised *Effect* in the *Effect Stack*, but each one is applied with different type-argument. For example:
  ```scala
  State[Int] with State[String]
  ```
  It will be discussed in [§. Tag Conflicts](#tag-conflicts).


## 4\. Computation

A *Computation* is any value of a type derived from `Effectful[+A, -U]` trait.  
* Parameter `A` is the result type of the *Computation*.  
* Parameter `U` is the *Effect Stack* of the *Computation*. It's meaning is to act as a registry of *Effects* that will have to be [§. handled](#6-handling-effects), before the result of the *Computation* can be obtained.

Example:
```scala
type MyComputation = Effectful[Foo, State[Double] with Error[String] with Choice]
```
Same example, but using `!!`, an **infix type alias** for `Effectful`:
```scala
type MyComputation = Foo !! State[Double] with Error[String] with Choice
```

The latter is more readable, as long as one remembers that:
* Precedence of `!!` is lower than of `with`, so `A !! U with V` means `A !! (U with V)`
* Precedence of `!!` is higher than of `=>`, so `A => B !! U` means `A => (B !! U)`

### 4\.1\. Return

The simplest *Computation* is constructed by `Return(x)` case class, where `x` is any value.  

`Return(_)` is similar to `Pure(_)`, `Some(_)`, `Right(_)`, `Success(_)` from other monads. Except in Skutek, `Return(_)` is shared for all possible *Effects*.

A `Return` has empty *Effect Stack*:
```scala
// assuming: 
val a: A = ???

// let:
val eff = Return(a)

// we get:
eff: A !! Any
```

Also, `Return()` is an abbreviation of `Return(())`.

### 4\.2\. Operations

An *Operation* is an elementary *Computation*, specific for an *Effect*.

*Operations* originate from *Effect Definitions*, where they are defined as **dumb case classes**, indirectly inheriting from `Effectful` trait.

Examples:

|Constructor of *Operation* | It's *Effect Stack* | Type of the *Computation*|
|---|---|---|
|`Get[Double]`        |`State[Double]`   | `Double !! State[Double]`| 
|`Put(1.337)`         | same as above    | `Unit !! State[Double]`| 
|`Tell("Hello")`      |`Writer[String]`  | `Unit !! Writer[String]`|
|`Choose(1 to 10)`    |`Choice`          | `Int !! Choice`|

Nullary *Operations* require explicit type parameter, like in the case of `Get[Double]`.


### 4\.3\. Composing Computations

*Computation* is a monad, so standard `map`, `flatMap` and `flatten` methods are available.

When 2 *Computations* are composed using `flatMap`, their *Effect Stacks* are **automatically merged**, by type intersection.

Example:
```scala
// assuming:
val eff1: A !! U1 = ???
val eff2: B !! U2 = ???  // dependency of eff2 on eff1 is ommited for the sake of clarity

// let:
val eff3 = eff1.flatMap(_ => eff2)  

// we get:
eff3: B !! U1 with U2 
```

---

Two *Computations* can also be composed parallelly, using product operator: `*!`. *Computation's* result type is a pair of result types of the operands.  

The parallelism is potential only. Whether it's exploited or not, deppends on *Handlers* used to run the resulting *Computation*.

Just like in case of `flatMap`, *Effect Stack* of the product comes from merging *Effect Stacks* of the operands.

Example:
```scala
// assuming:
val eff1: A !! U1 = ???
val eff2: B !! U2 = ???

// let:
val eff3 = eff1 *! eff2

// we get:
eff3: (A, B) !! U1 with U2 
```

Additional 2 operators are provided: `*<!` and `*>!`. They work just like `*!`, with addition of projecting resulting pair to its first and second component respectively.

## 5\. Handler

*Handler* is an object, that has ability to [§. handle](#6-handling-effects) an *Effect* (or multiple *Effects*).  

Terminology: When it is stated that a *Handler* handles an *Effect Stack*, it's done to emphasize that a *Handler* can handle **multiple** *Effects* at once. It should not be interpreted as statement, that *Handler* can handle this particular *Effect Stack* only.

Trait `Handler` is the supertype of all *Handlers*. It's dependent on 2 member types:
* `Handler#Effects` - An *Effect Stack* - set of *Effects* that can be handled by this *Handler*.
* `Handler#Result[X]` - A type-level function, describing how *Computation's* result type is transformed during handling the *Effect Stack*.


### 5\.1\. Elementary handlers

Those are *Handlers* that originate from *Effect Definitions*. Each one is dedicated to handling **single** specific *Effect*. 

Examples:

| Constructor of *Handler* | `Handler#Effects` | `Handler#Result[A]` |
|---|---|---|
|`StateHandler(42.0)`|`State[Double]`| `(A, Double)` |
|`ErrorHandler[String]`|`Error[String]`|`Either[String, A]`|
|`ChoiceHandler`|`Choice`|`Vector[A]`|

### 5\.2\. Composed handlers

Multiple *Handlers* can be associatively composed using `+!` operator, forming *Handler*
that can handle bigger *Effect Stack*. 

For example, *Handler*:
```scala
val handler = StateHandler(42.0) +! ErrorHandler[String] +! ChoiceHandler
```
Can handle the following *Effects Stack*:

```scala
State[Double] with Error[String] with Choice
```

The order of composition matters:  
* The order of occurence of the operands is: outermost effect first, the innermost effect last.  
* However, the order of actual handling is **reverse** of that: the innermost effect is handled first, the outermost effect is handled last. This reversed order reflects the order of applications of `Handler#Result[X]` from each operand.

## 6\. Handling Effects

Handling an *Effect* (or *Effect Stack*), is an act of using a *Handler* to transform a *Computation* to another one. 

During this transformation, following things are observed:
* *Computation's* *Effect Stack* is reduced.  
  Precisely, **a set difference** is performed: from *Computation's* *Effect Stack*, the *Handler's* *Effect Stack* is subtracted. Possibly even leaving empty set in the outcome.
* *Computation's* result type is transformed by `Handler#Result[_]` type-level function.
---
After all *Effects* are handled, *Computation's* *Effect Stack* is empty (i.e. provable to be `=:= Any`).
Then, the *Computation* is ready to be **executed**. The obtained value is finally liberated from the monadic context:
```scala
// assuming:
eff: A !! Any

// let:
val a = eff.run   

// we get:
a: A
```
### 6\.1\. Full handling

The **easiest** way of using *Handlers*, is to handle entire *Effect Stack* at once: 
1. Create composed *Handler*, covering all *Effects* in the *Computation's* *Effect Stack*.
2. Handle the *Effects* and execute the *Computation*, all in one call.

Example:
```scala
// assuming:
eff: Int !! State[Double] with Choice = ???

// Step 1.
val handler = StateHandler(1.337) +! ChoiceHandler

// Step 2.
val result = handler.run(eff) 

val result = eff.runWith(handler)   // alternative syntax, with eff and handler flipped

// we get:
result: (Vector[Int], Double)
```

### 6\.2\. Local handling

In practical programs, it's often desirable to handle only a subset of
*Computation's* *Effect Stack*, leaving the rest to be handled elsewhere.
This allows to encapsulate usage of local *Effect(s)* in a module, while 
still exporting effectful API that uses other, public *Effect(s)*.

Such situation (although on small scale) can be seen in the [Queens](./examples/src/main/scala/skutek_examples/Queens.scala) example:
* The `State` *Effect* is used and [handled](./examples/src/main/scala/skutek_examples/Queens.scala#L39) internally.
* The `Choice` *Effect* is used and [exported](./examples/src/main/scala/skutek_examples/Queens.scala#L28) in function's result.
* The `Choice` *Effect* is finally [handled](./examples/src/main/scala/skutek_examples/Queens.scala#L10) by the client. By having control of the *Handler*, the client can decide whether it wants to enumerate all solutions, or just get the first one that is found.
---
There are 2 ways of handling *Effects* locally: one is simpler, the other is safer. The safety issue is explained in the [§. Tag Conflicts](./README.md#tag-conflicts).

There is another complication. Unfortunately, in both cases you won't be able to rely on type inference. It will be necessery to explicitly pass an *Effect Stack* as type parameter to handling methods. 

Moreover, this explicit *Effect Stack* has to consist of *Effects* that are going to **remain unhandled**, not the ones that are being **handled** in the call. That's rather inconvenient, but we can do nothing about it.

#### 6\.2\.1\. The simpler way

```scala
// assuming:
val eff: Int !! State[Double] with Reader[Boolean] with Error[String] with Choice = ???
// continued...
```
We are going to handle *Effects* `State[Double]` and `Choice`.  
We are going to leave *Effects* `Reader[Boolean]` and `Error[String]` unhandled.

```scala
// ...continued
// let:
val handler = StateHandler(1.337) +! ChoiceHandler

val eff2 = handler.handleCarefully[Reader[Boolean] with Error[String]](eff) 

val eff2 = eff.handleCarefullyWith[Reader[Boolean] with Error[String]](handler)  // alternative syntax

// we get:
eff2: (Vector[Int], Double) !! Reader[Boolean] with Error[String]
```

#### 6\.2\.2\. The safer way

```scala
// assuming:
val eff = ... // same as in previous example 

// let:
val hander = // same as in previous example 

val eff2 = handler.fx[Error[String]].fx[Reader[Boolean]].handle(eff) 

val eff2 = eff.fx[Error[String]].fx[Reader[Boolean]].handleWith(handler)  // alternative syntax

// we get:
eff2: ... // same as in previous example
```
The `fx` method is defined for both `Effectful` and `Handler` traits. 

The chain of `fx` method calls is a Builder Pattern. It has to be used to enumerate each *Effect* that is has to remain unhandled, before terminating the chain with `handle/handleWith` call. Otherwise, it's not supposed to typecheck.

Also, the type passed to `fx` has to be single *Effect*. Passing an *Effect Stack* of length other than `1`, won't work.

# Part II - Predefined Effects

## Reader Effect
||||
|---|---|---|
**Effect:** |`Reader[T]` | **Purposes:** <br/>Purely functional equivalent of **read-only** global variable. <br/>Scoped constants. Dependency injection.
**Operation:** | `Ask[T]` |  Summons a value of type `T` out of nowhere. Let the handler worry how to deliver it. <br/> The summoned value (traditionally called "an environment") is always the same, unless overriden with `Local`.  
**Operation:** | `Local(f)(eff)` | Executes inner computation `eff` (of any type and with any effects), where the "environment" is locally modified by pure function `f` of type `T => T`.
**Handler:** |`ReaderHandler(x)` | Provides the initial "environment" of type `T`. 

## Writer Effect
||||
|---|---|---|
**Effect:** | `Writer[T]` | **Purposes:** <br/>Purely functional equivalent of **write-only** global variable. <br/>Write-only accumulator. Log.
**Operation:** | `Tell(x)` | Dumps a value of type `T` somewhere. Let the handler worry what to do with it.  
**Handler:** | `WriterHandler.seq[T]` | Handles the effect by storing the dumped values in a `Vector[T]`.
**Handler:** | `WriterHandler.strings` | Specialized `WriterHandler.seq[String]`.
**Handler:** | `WriterHandler.monoid(zero, add)` | Creates handler that accumulates dumped values as if `T` was a Monoid.<br/> `zero : T` is the neutral element, `add : (T, T) => T` is the binary operator.

## State Effect
||||
|---|---|---|
**Effect:** | `State[T]` | **Purpose:** Purely functional equivalent of mutable global variable.
**Operation:** | `Get[T]` | Gets the current value of the state.
**Operation:** | `Put(x)` | Overwrites the current value of the state.
**Operation:** | `Modify(f)` | Modifies the current value of the state, by applying a pure `T => T` function to it.
**Handler:** | `StateHandler(x)` | Provides the initial value of the state. Returns computed value in a pair with the final state.
**Handler:** | `StateHandler(x).exec` | Returns the final state only.
**Handler:** | `StateHandler(x).eval` | Forgets the final state.

## Maybe Effect
||||
|---|---|---|
**Effect:** | `Maybe` | **Purpose:** Provides ability to stop the computation, without returning any value <br/> Similar to `Option[_]`, but composable with other effects.
**Operation:** | `Naught` | Aborts the computation. Similar to `None`. 
**Handler:** | `MaybeHandler` | Result type of the computation in wrapped in an `Option[_]`

There is no counterpart of `Some(x)`. `Return(x)` can be used.

Use `.toEff` extension method, to convert an `Option[T]` to `T !! Maybe`.

## Error Effect
||||
|---|---|---|
**Effect:** | `Error[T]` | **Purpose:** Purely functional equivalent of throwing & handling exceptions. <br/> Similar to `Either[T, _]`, but composable with other effects. <br/> Similar to `Try[_]`, but the error value doesn't have to be `Throwable`.
**Operation:** | `Wrong(x)` | Aborts the computation with an error value of type `T`. Similar to `Left(x)`
**Handler:** | `ErrorHandler[T]` | Result type of the computation in wrapped in an `Either[T, _]`

There is no counterpart of `Right(x)` or `Success(x)`. There is no need for any indicating of "lack of error", but `Return(x)` can be used.

Use `.toEff` extension method, to convert an `Either[A, B]` to `B !! A`.

## Validation Effect
||||
|---|---|---|
**Effect:** | `Validation[T]` | **Purpose:** Similar to `Error` effect, but allows accumulation of errors from mutually independent computations.
**Operation:** | `Invalid(x)` | Aborts the computation with an error value of type `T`. 
**Handler:** | `ValidationHandler[T]` | Result type of the computation in wrapped in an `Either[Vector[T], _]`

For error accumulation to actually happen, computations must be composed parallelly. Otherwise, `Validation` will abort the computation at the first error.

## Choice Effect
||||
|---|---|---|
**Effect:** | `Choice` | **Purpose:** Seqential search with backtracking (in depth first order).
**Operation:** | `Choose(xs)` | Forks the computation, for each `x` from `xs` (where `xs` is any `Iterable[_]`).
**Operation:** | `Choose.from(a, b, c)` | Same as `Choose(List(a, b, c))`.
**Operation:** | `NoChoice` | Aborts the current fork with no result. <br/>Same as `Choose(List())`.
**Handler:** | `ChoiceHandler` | Accumulates the results of each fork in a `Vector[_]`.
**Handler:** | `ChoiceHandler.FindFirst` | Stops at first fork that ends with a result. Returns an `Option[_]`.

## Concurrency Effect
||||
|---|---|---|
**Effect:** | `Concurrency` | **Purpose:** Wrapper of `Future` from Scala's stanard library.
**Operation:** | `Run(x)` | Evaluates `x` asynchronously. No `ExecutionContext` required :sunglasses:.
**Operation:** | `RunEff(eff)` | Same as `Run(eff).flatten`. The `eff` can be any *Computation* with any *Effect Stack*
**Handler:** | `ConcurrencyHandler()` | Requires implicit `ExecutionContext`. Returns a `Future[_]` of computation's result.
**Handler:** | `ConcurrencyHandler.await(timeout)` | 

**Warning:** `Concurrency` must be handled as the final (outermost) *Effect* in the *Effect Stack*. Failing to do so, will result in `Unhandled Effect` error.

## Eval & Trampoline

These aren't true *Effects*, and they don't require any *Handler*.

`Eval(x)` - Is like `Return(x)`, but the `x` is evaluated lazily. *Effect Stack* is empty.

`Trampoline(eff)` - Is like standalone `eff`, but prevents stack overflow, if `eff` is recursive. *Effect Stack* is the same as of `eff`.


# Part III. Advanced Topics

## Traversing

Traversing is transforming a collection-of-*Computations* into a *Computation*-of-collection.

Example:
```scala
// assuming:
val effs: List[Int !! Validation[String]] = ???

// let:
val eff = effs.parallelly // or:
val eff = effs.serially

// we get:
eff: List[Int] !! Validation[String]
```

By "collection", we mean `Option`, `Either` or any subclass of `Iterable`.  
Skutek defines extension methods for traversing them: 
* `.parallelly` - Essentially, it's a fold with `*!`.  
  The parallelism is potential only. Whether it's exploited or not, deppends on *Handlers* used to run the resulting *Computation*.
* `.serially` - Essentially, it's a **lazy** fold with `flatMap`.  
  By "lazyness" here, we mean that abortable *Effects* (e.g. `Maybe`, `Error` or `Validation`) may abort executing the whole computation on the first error/failure/etc. encountered in the sequence.

Obviously, for `Option` and `Either`, the difference between `.serially` and `p.arallely` vanishes.

---

In case we want to traverse the collection only for the *Effects*, and discard result of each element of the collection, there are more efficient alternatives:
* `.parallellyVoid` 
* `.seriallyVoid`

They are more efficient, because they avoid construction of useless collection of `()` values. Also, the result type of the *Computation* is overriden as `Unit`.

```scala
// assuming:
val effs: List[Int !! Validation[String] with Writer[String]] = ???

// let:
val eff = effs.parallellyVoid // or:
val eff = effs.seriallyVoid

// we get:
eff: Unit !! Validation[String] with Writer[String]
```
## Parallellism

TODO

```scala
eff1 *! eff2 ==== for { a <- eff1; b <- eff2 } yield (a, b)
```

TODO



## Tagging Effects

As explained in the [§. beginning](#2-effect), the role of *Effect* is to be type-level name. Tagging allows overriding that name, so that multiple instances of the same *Effect* can coexist in one *Effect Stack*. 

Such name-overriding is done by attaching unique *Tag*. A *Tag* is required to be unique **type**, as well as unique **value**. The easiest way of definning *Tags*, is with `case object`. For example:
```scala
case object TagA
case object TagB
```
Attaching a *Tag* to an *Effect* is done using `@!`, a **type-level** operator.

For example, an *Effect Stack* with 2 `State` *Effects*, each given separate *Tag*, looks like this:
```scala
(State[Int] @! TagA) with (State[Double] @! TagB)
```
For such *Effects* to be usable, we need ability to also tag *Operations* and *Handlers*, so that they are compatible with the tagged *Effects*. This tagging is also done using `@!` operator. However, this time it operates on **values**, instead of types.

Example of tagged *Operation*:
```scala
val eff = Get[Int] @! TagA
```
Example of tagged *Handler*:
```scala
val handler = StateHandler(42) @! TagA
```

In 2 above examples, the *Effect Stack* of `eff` and `handler` is `State[Int] @! TagA`

---

Now, full example combining 2 tagged *Effects*:
```scala
case object TagA
case object TagB

val eff = for {
  a <- Get[Int] @! TagA
  b <- Get[Double] @! TagB
  c = a * b
  _ <- Put(c) @! TagB
} yield s"$a * $b = $c"

val handler = (StateHandler(42) @! TagA) +! (StateHandler(0.25) @! TagB)

val result = handler.run(eff)

// we get:
result: ((String, Double), Int) 
result == (("42 * 0.25 = 10.5", 10.5), 42)
```

---

Actually, *Tags* were always there. What appeared as untagged entities (*Effects*, *Operations* and *Handlers*), were in fact entities tagged with **implicit** *Tags*. In current implementation, Skutek uses `scala.reflect.ClassTag[Fx]` as the default *Tag* for *Effect* `Fx`.

## Synthetic Operations

It's impossible to attach a *Tag* to a composed *Computation*. Neither to a composed *Handler* for the matter, but it wouldn't make sense anyway.

For example:
```scala
// assuming:
def modify[S](f : S => S) = for { 
  s <- Get[S]
  _ <- Put(f(s)) 
} yield ()

val inc = (i: Int) => i + 1

// let:
val eff = modify(inc) @! TagA
```
The last line won't compile.

To alleviate the problem, we can use `SyntheticOperation`. This allows creating of operations, that are both taggable, and composed of simpler operations. This mechanism is quite complex and may be changed in future versions. For now, read the sources:

- How `State` effect defines `Modify` operation: [link](core/src/main/scala/skutek/State.scala#L11-L17).
- How `Reader` effect defines `Local` operation: [link](core/src/main/scala/skutek/Reader.scala#L11-L19).


## Tag Conflicts

Not all *Effect Stacks* are valid. Skutek requires that each *Effect* in an *Effect Stack* has unique [§. tag](#tagging-effects). 

For example, the following *Effect Stack* is invalid, because `TagA` is used to tag 2 different *Effects*:
```scala
(Reader[String] @! TagA) with (State[Int] @! TagA)
```
For untagged *Effects*, this rule is applied to their **implicit** tags.

For example, the following *Effect Stack* is invalid:

```scala
State[Int] with State[String]
```
because implicit tags of those 2 *Effects* are the same (in current implementation: `scala.reflect.ClassTag[State[_]]`).

---

Unfortunately, Skutek is unable to detect invalid *Effect Stacks* at **compile-time**. Attempt to run a computation with invalid *Effect Stack* would result in `asInstanceOf` error, or filed `match`, somewhere deep inside effect interpreter loop.

The only defense mechanism Skutek has, is employed at **run-time**. Type information is utilised to make detection of invalid *Effect Stacks* at predictable, static spots of the program: where **handlers** are put to work. 

In short, construction of *Computations* with invalid *Effect Stacks* may go unnoticed by the compiler. However, it will be impossible to construct a proper, typechecking *Handler* to run it.

For example, construction of handlers:

```scala
val invalidHandler1 = StateHandler(42) +! StateHandler("Hello")

val invalidHandler2 = (ReaderHandler(42) @! TagA) +! (StateHandler("Hello") @! TagA)
```
will fail at runtime.

---

This safety problem is the reason, why 2 ways of [§. local handling](#62-local-handling) are provided in Skutek:
* [§. The safer way](#622-the-safer-way) **compile-time** forces the user to decompose his *Effect Stack* into individual *Effects* (using Builder Pattern), so that tag uniqueness can be verified by Skutek at **run-time**.
* [§. The simpler way](#621-the-simpler-way) doesn't use such discipline, so it may leak invalid *Effect Stacks* undetected. Hence the name: `handleCarefully`.

## Mapped handlers

An elementary *Handler* can be transformed to another *Handler*, by using a [polymorphic function](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#polymorphic-function-values) (a.k.a. [natural transformation](https://apocalisp.wordpress.com/2010/07/02/higher-rank-polymorphism-in-scala/)), that will be applied to postprocess the value obtained from [§. handling](#6-handling-effects). 

Mapped handler handles the same *Effect* as the original, but typically have different `Handler#Result[X]` type.

For example, `StateHandler` has 2 utility methods: `.eval` and `.exec`, each of which constructs mapped *Handler*. The postprocessing function, in this case, is projection of pair to its first and second element respectively:

| Handler construcion | `Handler#Result[A]` | Comment |
|---|---|---|
|`StateHandler(42.0)`      | `(A, Double)`| The original *Handler* |
|`StateHandler(42.0).eval` | `A`| Mapped *Handler* that forgets the final state |
|`StateHandler(42.0).exec` | `Double`| Mapped *Handler* that keeps the final state only |


TODO *how to create your own mapped handlers*

## Defining your own Effect

TODO 

This part is the most likely to be modified in future versions of Skutek.

