package wired

import cats._
import cats.data.Kleisli
import cats.implicits._

import scala.language.implicitConversions

object wiring {

  type Wiring[I, O] = Kleisli[Eval, I, O]
  type ->>[I, O] = Wiring[I, O]
  type <<-[O, I] = Wiring[I, O]
  type Wired[O] = Unit ->> O

  def ask[I]: I->>I = Kleisli.ask[Eval, I]

  def wire[I, O](value: O): Wired[O] = Kleisli.lift(Eval.now(value))

  implicit class WireSyntax[O](val value: O) extends AnyVal {
    def wire[OO >: O]: Wired[OO] = wiring.wire(value)
    def ->>[OO >: O]: Wired[OO] = wire
  }

  implicit class WiringSyntax[I, O](val wiring: I->>O) extends AnyVal {
    def zip[R](right: I->>R): I->>(O,R) = wiring |@| right map (_ -> _)
    def =>>[R](f: O => R) = wiring map f
    def <<=[R](f: R => I) = wiring contramap f
    def singleton: I->>O = wiring.mapF(_.memoize)
    def get(i: I): O = wiring.run(i).value
  }

  implicit class IgnoringSyntax[O](val wired: Wired[O]) extends AnyVal {
    def ignoring[I]: I->>O = wired.local(_ => ())
  }

  implicit def ignoring[A, B](wired: Wired[A]): B->>A = wired.ignoring

  implicit def widening[A, B, D >: B](wiring: A->>B): A->>D = wiring.widen

  // Syntactic sugar:

  implicit class WireSyntaxF1[I1, O](val f: I1 => O) extends AnyVal {
    def wire[I2 <: I1](wiring: I2->>I1): I2->>O = wiring map f
  }

  implicit class WireSyntaxF2[I1, I2, O](val f: (I1, I2) => O) extends AnyVal {
    def wire[II](w1: II->>I1, w2: II->>I2) = w1 |@| w2 map f
  }

  implicit class WireSyntaxF3[I1, I2, I3, O](val f: (I1, I2, I3) => O) extends AnyVal {
    def wire[II](w1: II->>I1, w2: II->>I2, w3: II->>I3) = w1 |@| w2 |@| w3 map f
  }

  implicit class WireSyntaxF4[I1, I2, I3, I4, O](val f: (I1, I2, I3, I4) => O) extends AnyVal {
    def wire[II](w1: II->>I1, w2: II->>I2, w3: II->>I3, w4: II->>I4) = w1 |@| w2 |@| w3 |@| w4 map f
  }

}
