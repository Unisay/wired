# Example of a dependency injection approach in scala

## Glossary:
- `Component`: Some type that can be instantiated (initialized) with configuration values as its state passed via constructor. Might be represented by trait with implementing class or just by a [case] class. 
- `Wiring[A]`: Data type capable of instantiating the component of type `A` wiring ith its dependencies, recursively
- `Module`: group of `Wirings`.

## Features:
- [x] Functional: immutable and lazy
- [x] Type-safe: compile-type verification with readable error messages
- [x] Configurable: supports multiple `Module`s with possibility to override specific `Wiring`s (useful for testing)
- [ ] Handles cyclic-dependencies
