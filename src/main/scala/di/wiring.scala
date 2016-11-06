package di

import cats._
import cats.data.Kleisli
import cats.implicits._

object wiring {

  type Wiring[I, O] = Kleisli[Eval, I, O]
  type Wired[O] = Wiring[Unit, O]
  type Requires[O, I] = Wiring[I, O]

  def ask[I]: Wiring[I, I] = Kleisli.ask[Eval, I]

  def wire[I, O](value: O): Wired[O] = Kleisli.lift(Eval.now(value))

  implicit class WireSyntax[O](val value: O) extends AnyVal {
    def wire[OO >: O]: Wired[OO] = wiring.wire(value)
  }

  implicit class SingletonSyntax[I, O](val wiring: Wiring[I, O]) extends AnyVal {
    def singleton: Wiring[I, O] = wiring.mapF(_.memoize)
  }

  implicit class IgnoringSyntax[O](val wired: Wired[O]) extends AnyVal {
    def ignoring[I]: Wiring[I, O] = wired.local(_ => ())
  }

  // Syntactic sugar:

  implicit class WireSyntaxF1[I1, O](val f: I1 => O) extends AnyVal {
    def wire[I2 <: I1](wiring: Wiring[I2, I1]): Wiring[I2, O] = wiring map f
  }

  implicit class WireSyntaxF2[I1, I2, O](val f: (I1, I2) => O) extends AnyVal {
    def wire[II](w1: Wiring[II, I1], w2: Wiring[II, I2]) = w1 |@| w2 map f
  }

  implicit class WireSyntaxF3[I1, I2, I3, O](val f: (I1, I2, I3) => O) extends AnyVal {
    def wire[II](w1: Wiring[II, I1], w2: Wiring[II, I2], w3: Wiring[II, I3]) = w1 |@| w2 |@| w3 map f
  }

  implicit class WireSyntaxF4[I1, I2, I3, I4, O](val f: (I1, I2, I3, I4) => O) extends AnyVal {
    def wire[II](w1: Wiring[II, I1], w2: Wiring[II, I2], w3: Wiring[II, I3], w4: Wiring[II, I4]) =
      w1 |@| w2 |@| w3 |@| w4 map f
  }

}
