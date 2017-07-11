# Skutek: extensible effects without (heavy) lifting

Extensible effects for Scala



### Features

- Simplicity of use:
    - No need of defining the effect stack upfront. Just compose your computations, and the stack will grow as you go, with help of Scala's type inference.
    - No need of "lifting into the monad". Effect's operations (`Get[Int]`, `Put(42)`) are usable directly. 
    - Effect extensiblity is provided by classic, vanilla OOP inheritance.
    - Effect subtyping ‥‥
    
- Simplicity of implementation:
    - No dependencies on external libraries.
    - No clever type-fu, no macros. Mostly OOP style without use of mutablity.
     
- Practical stuff:
    - Predefined set of basic effects (`Reader`, `Writer`, `State`, `Choice`, `Maybe`, `Error`, `Validation`, `Trampoline`, `Concurrency`).
    - Ability to annotate effects with tags (labels), to allow inclusion of multiple instances of single effect in the stack (`@!` operator)
    - Potentially parallel execution of effects, without breaking monad laws (`*!` operator and `traverse` methods).
    - Support for `for` comprehension guards (`withFilter`), for effects compatible with filtering (`Maybe` and `Choice`).
    - Tested stack safety.    
    
- Caveats and limitations:
    - General infancy of the project.
    - Lack of performance analysis.
    - Rare occurences of false positives by Scala's linter ("inferred `Any`...")
    - Using patterns in `for` comprehensions can trigger surprising errors (well known Scala's wart, not specific to Skutek)
    - `Concurrency` is a lie.
    - **Type unsafety:** Certain class of malformed effect stacks (`Reader[Int] with Reader[String]`) are detected **at runtime** only, and no sooner than the first call to handler.

### Setup

TBD

### Examples

TBD
