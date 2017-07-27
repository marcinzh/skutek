
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
// Same as above, but uses `!!` type alias of Effectful[_, _].
// Precedence of `!!` is lower than of `with`
// but with higher than of `=>`.
```

# Computations
```scala

Return(a)  // Equivalent of Pure(_), Some(_), Right(_), Success(_) in other monads. 
           // Common for all effects in Skutek.
           // Effect stack is empty (Any).

eff1.flatMap(x => eff2)  // Effect stacks of eff1 and eff2 can be different.
                         // Scala's type inference combines them to get the final effect stack.
                         // Conceptually, it's an union of 2 sets of effects, but in Skutek's 
                         // representation, it's an intersection of 2 types. Weird, isn't it?

eff1 *! eff2   // Potentially parallel composition of 2 computations, retuning a pair.
               // Actual parallelism depends on a handler stack used later.
               // Effects stacks of eff1 and eff2 are joined, just like it happens with flatMap.

eff1 *<! eff2  // Same, but projects the resulting pair to its left component
eff1 *>! eff2  // Same, but projects the resulting pair to its right component
```

# Operations

TBD

# Handlers

TBD

# Traversing

```scala
// asuming:
effs : SomeCollection[A !! U] 
// where SomeCollection[_] is an Iterable[_], Option[_] or Either[Foo, _]

effs.traverse     // Potentially parallel execution of effectful computations, combining their results.
                  // Returns SomeCollection[A] !! U.
                  
effs.traverseVoid // Same, but does it only for the effects, ignoring each result.
                  // More efficient, because avoids constructing useless collection of Unit values.
                  // Returns Unit !! U
                  
effs.traverseLazy // Similar to .traverse, but sequences effects in a chain, even though elements 
                  // themselves are independent. Prevents parallelism.
                  // Abortable effects, like Maybe, Error or Validation can abort the computation
                  // on the first error/failure/etc.
              
effs.traverseLazyVoid // Obvious.
               
// In the next version .traverse will be renamed to .parallelly and .traverseLazy to .serially
                  

```

# Tagging

TBD
