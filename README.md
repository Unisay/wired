# Example of a dependency injection approach in scala

## Requirements:
- [x] In any place of a program be able to wire a sub-component
- [x] Readable compile-time error messages
- [x] Handle alternative wirings (for testing)
- [ ] Handle cyclic-dependencies


## Glossary:
- `Component`: Some type that can be instantiated (initialized) with configuration values as its state passed via constructor. Might be represented by trait with implementing class or just by a [case] class. 
- `Module`: certain way of instantiating components and wiring them together. An application can use multiple modules whenever its needed to instantitate and wire components differently. Canonical exmple: Test module used for testing.
- `Wiring[A]`: Data type capable of instantiating a `Component` (with its dependencies, recursively)
