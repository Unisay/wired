# Wired

Functional config/dependency injection tool for Scala

## Features:
- [x] Functional: composable, immutable and lazy.
- [x] Type-safe: compile-type verification with readable error messages.
- [x] Configurable: supports multiple `Module`s with possibility to override specific `Wiring`s (useful for testing)
- [x] Non-intrusive: components don't know how exactly they are wired together.

## Glossary:
- `Component` is some type that can be instantiated (initialized) with configuration values as its state passed via constructor. Might be represented by trait with implementing class or just by a [case] class. 
- `Wiring[A]` is a data type capable of instantiating the component of type `A` recursively wiring its dependencies.
- `Module` is a group of `Wiring`s.

## Implementation:

The central concept is `Wiring`

```scala
  type Wiring[I, O] = cats.data.Kleisli[cats.Eval, I, O] 
  // Wiring[Config, Application]
  // given a Config it can produce an Application
  
  type Wired[O] = Wiring[Unit, O] 
  // For types that require no configuration, like constants.
  // example: Wired[ExecutionContext]  
  
  type Requires[O, I] = Wiring[I, O] 
  // For infix notation, like: `Application Requires Config`
  // given a Config it can produce an Application
```

Assuming the following setup:

```scala
import wired.wiring._

trait C
case class A(c: C)
case class B(c: C)
case class D(a: A, b: B)
```

Wirings are defined like this:

```scala
// Define a wiring that given C produces a new instance of type A
val ca: Wiring[C, A] = ask[C] map A

// Define wiring using alternative syntax
// Given C it produces a new instance of type B
val cb: B Requires C = B.wire(ask[C])

// Define a singleton wiring that requires C and produces same instance of A each time its evaluated
val singletonA: Wiring[C, A] = A.wire(ask[C]).singleton

// Define a constant wiring that requires nothing in order to produce String
val const: Wired[String] = "Constant".wire // Wired[String] is equivalent to Wiring[Unit, String]
```

Wirings that require same type can be composed:

```scala
import cats.syntax.cartesian._

// Compose first and second wirings into product
val cab: (A, B) Requires C = ca |@| cb map (_ -> _)

// Compose first and second wirings into product type D using "sweet" syntax
val cd: D Requires C = D.wire[C](ca, cb) 
```

"Constant" wirings that require nothing (technically Unit) 
could be composed with wirings that require any other type:

```scala
val cac: Wiring[C, (A, String)] = ca |@| const map (_ -> _)
```

And evaluated:

```scala
val c = new C { override def toString = "C" }

println(ca.run(c).value) // prints: A(C)
println(ca(c).value)     // prints: A(C), shorter syntax
println(ca.get(c))       // prints: A(C), alternative syntax

println(cb.get(c))      // prints: B(C)
println(cab.get(c))     // prints: (A(C),B(C))
println(cd.get(c))      // prints: D(A(C),B(C))
println(const.get(()))  // prints: Constant 
println(cac.get(c))     // prints: (A(C),Constant) 
```
