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
// example: Wiring[Config, Application]
// Given a Config wiring produces an Application

// Aliases for infix notation
type ->>[I, O] = Wiring[I, O] // Config ->> Application
type <<-[O, I] = Wiring[I, O] // Application <<- Config

// Alias for types that require no configuration, like constants:
type Wired[O] = Unit ->> O // example: Wired[ExecutionContext]  
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
val ca: C ->> A = ask[C] map A
val ca: C ->> A = ask[C] ==> A // ==> is an alias for map

// Define wiring using alternative syntax
// Given C it produces a new instance of type B
val cb: C ->> B = B.wire(ask[C])

// Define a singleton wiring that requires C and produces same instance of A each time its evaluated
val singletonA: C ->> A = A.wire(ask[C]).singleton

// Define a constant wiring that requires nothing in order to produce String
val const: Wired[String] = "Constant".wire // Wired[String] is equivalent to Wiring[Unit, String]
```

Wirings that require same type can be composed:

```scala
import cats.syntax.cartesian._

// Compose first and second wirings into product
val abc: (A, B) <<- C = ca zip cb

// Compose first and second wirings into product type D using "sweet" syntax
val cd: C ->> D = D.wire[C](ca, cb) 
```

"Constant" wirings that require nothing (technically Unit) 
could be composed with wirings that require any other type:

```scala
val cac: C ->> (A, String) = ca |@| const map (_ -> _)
```

And evaluated:

```scala
val c = new C { override def toString = "C" }

println(ca.run(c).value) // prints: A(C)
println(ca(c).value)     // prints: A(C), shorter syntax
println(ca.get(c))       // prints: A(C), alternative syntax

println(cb.get(c))       // prints: B(C)
println(abc.get(c))      // prints: (A(C),B(C))
println(cd.get(c))       // prints: D(A(C),B(C))
println(const.get(()))   // prints: Constant 
println(cac.get(c))      // prints: (A(C),Constant) 
```

For an extended usage example see [Main.scala](https://github.com/Unisay/wired/blob/master/src/main/scala/wired/Main.scala)
