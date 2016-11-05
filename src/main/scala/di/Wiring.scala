package di

import cats._
import cats.data.Kleisli

trait Module {
  def wired[A]: A
}

object Wiring {

  type Wiring[I, O] = Kleisli[Eval, I, O]
  type Wired[O] = Wiring[Unit, O]
  type Requires[O, I] = Wiring[I, O]

  def ask[I]: Wiring[I, I] = Kleisli.ask[Eval, I]

  def prototype[I, O](value: O): Wired[O] = Kleisli.lift[Eval, Unit, O](Eval.now(value))

  def singleton[I, O](value: => O): Wired[O] = Kleisli.lift[Eval, Unit, O](Eval.later(value))

  implicit class WiringSyntax[O](val wiring: Wiring[Unit, O]) extends AnyVal {
    def ignoring[A]: Wiring[A, O] = wiring.local(_ => ())
  }

  implicit class WireSyntax[O](val value: O) extends AnyVal {
    def prototype[OO >: O]: Wired[OO] = Wiring.prototype(value)
    def singleton[I, OO >: O]: Wired[OO] = Wiring.singleton(value)
  }

}
