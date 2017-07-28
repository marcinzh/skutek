
# The Monad

```scala
type Eff = Effectful[Foo, State[Double] with Error[String] with Choice]

// where:

Effectful[+A, -U]  // Is a type of computation returning type A, 
                   // in the context of set of effects U (effect stack).

State[Double] with Error[String] with Choice // Is an example of effect stack.
                                             // The order of effects doesn't matter, but 
                                             // we call it "stack" anyway.

State[Double]
Error[String]
Choice         
// These are phantom types, each one identifying particular effect.
               

type Eff = Foo !! State[Double] with Error[String] with Choice
// Same as above, but uses `!!`, an infix type alias of Effectful[_, _].
// Precedence of `!!` is lower than of `with`
// but with higher than of `=>`.
```

# Computations

Computations are values of type `Effectful[+A, -U]`

```scala

Return(a)  // Equivalent of Pure(_), Some(_), Right(_), Success(_) in other monads. 
           // Common for all effects in Skutek.
           // Effect stack is empty (Any).

// assuming:
eff1 : A !! U1
eff2 : B !! U2

eff1.flatMap(x => eff2)  // Effect stacks of eff1 and eff2 can be different.
                         // Scala's type inference combines them to get the final effect stack.
                         // Conceptually, it's an union of 2 sets of effects, but in Skutek's 
                         // representation, it's an intersection of 2 types. Weird, isn't it?

eff1 *! eff2   // Potentially parallel composition of 2 computations, retuning a pair.
               // Actual parallelism depends on the handler stack used later to run it.
               // Effects stacks of eff1 and eff2 are joined, just like it happens with flatMap.

eff1 *<! eff2  // Same, but projects the resulting pair to its left component
eff1 *>! eff2  // Same, but projects the resulting pair to its right component
```

# Operations

An operation is an elementary computation, specific for an effect.
Operations are defined as simple case classes, indirectly inheriting 
from `Effectful[_, _]` trait.

Examples:

|expression | its effect </br> (single element effect stack) | its supertype|
|---|---|---|
|`Get[Double]`        |`State[Double]`   | `Double !! State[Double]`| 
|`Put(1.337)`         | same as above    | `Unit !! State[Double]`| 
|`Tell("Hello")`      |`Writer[String]`  | `Unit !! Writer[String]`|
|`Choose(1 to 10)`    |`Choice`          | `Int !! Choice`|

Nullary operations require explicit type parameter, like in the case of `Get[]`.

# Handlers

Handler is an object, which has ability to handle effect (or effects). 

Handling an effect (or effects), is an act of removing some effect (or effects) from 
the computation's effect stack. Possibly, also transforming computation's result 
type in the process.

Handling effects is also the point, where **the order of effects** starts to matter.

After all effects are handled, computation's effect stack is empty (i.e. provable to be `=:= Any`)
Then, the computation is ready to be executed:
```scala
// assuming:
eff : A !! Any

eff.run   // returns A
```



### Elementary handlers
Every effect definiton provides a handler for its own effect. Examples:

| expression creating <br> handler instance | effect it handles  </br> (single element effect stack) | how handler transforms </br> computation's result type `A` |
|---|---|---|
|`StateHandler(42.0)`|`State[Double]`| `(A, Double)` |
|`ErrorHandler[String]`|`Error[String]`|`Either[String, A]`|
|`ChoiceHandler`|`Choice`|`Vector[A]`|

### Composing handlers
Multiple handlers can be associatively composed using `+!` operator, forming handlers 
that can handle bigger sets of effects. For example:

```scala
StateHandler(42.0) +! ErrorHandler[String] +! ChoiceHandler

// can handle effects:

State[Double] with Error[String] with Choice
```
### Full handling

The **easiest** way of using handlers, is to handle all effects at once: 
1. Create composed handler, covering all effects in the computation's effect stack.
2. Handle effects and execute the computation, all in one call.

Example:
```scala
// assuming:
eff : Int !! State[Double] with Choice

// Step 1.
val handler = StateHandler(1.377) +! ChoiceHandler

// Step 2.
handler.run(eff)     // returns: (Vector[Int], Double)

eff.runWith(handler) // alternative syntax
```

### Local handling
In practical programs, it's often desirable to handle only a subset of
computation's effect stack, leaving the rest to be handled elsewhere.
This allows to encapsulate usage of local effect(s) in a module.

must be augmented by explicit type.

Counterintuitively,

##### Shorter, but unsafe way

```scala
// assuming:
eff : Int !! State[Double] with Reader[Boolean] with Error[String] with Choice 

// Those are the effect we are going to leave unhandled:
type UnhandledEffects = Reader[Boolean] with Error[String]

// Making this type alias is not necessery, but it will make 
// our example less cluttered.

val handler = StateHandler(1.377) +! ChoiceHandler

handler.handleCarefully[UnhandledEffects](eff) 
// returns: (Vector[Int], Double) !! UnhandledEffects

eff.handleCarefullyWith[UnhandledEffects](handler) 
// alternative syntax
```

##### Safer, but more verbose way

```scala

eff : ... // same as in previous example 

val hander = // same as in previous example 

handler.fx[Error[String]].fx[Reader[Boolean]].handle(eff) 
// returns: same as in previous example 

eff.fx[Error[String]].fx[Reader[Boolean]].handleWith(handler) 
// alternative syntax
```

TBD



# Traversing

```scala
// assuming:
effs : SomeCollection[A !! U] 
// where: SomeCollection[_] is an Iterable[_], Option[_] or Either[Foo, _]

effs.traverse     // Potentially parallel execution of effectful computations, combining their results.
                  // Returns SomeCollection[A] !! U.
                  
effs.traverseVoid // Same, but does it only for the effects, ignoring each result.
                  // More efficient, because avoids constructing useless collection of Unit values.
                  // Returns Unit !! U
                  
effs.traverseLazy // Similar to .traverse, but sequences effects in a chain, even though elements 
                  // themselves are independent. Prevents parallelism.
                  // Abortable effects, like Maybe, Error or Validation can abort the whole
                  // computation, on the first error/failure/etc. encountered in the sequence.
              
effs.traverseLazyVoid // Obvious.
               
// In the next version .traverse will be renamed to .parallelly and .traverseLazy to .serially
                  

```

# Tagging

TBD
