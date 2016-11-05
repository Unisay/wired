package di

import cats._

trait WiringModule {
  def wired[A]: A
}

object Wiring {

  type Wiring[+A] = Eval[A]

  def wire[A](a: A): Wiring[A] = Eval.now(a)
  def singleton[A](a: A): Wiring[A] = Eval.later(a)

  implicit class OfSyntax[A, B](val f: A => B) extends AnyVal {
    def of(wiring: Wiring[A]): Wiring[B] = wiring.map(f)
  }

  implicit class WireSyntax[A](val a: A) extends AnyVal {
    def wire[AA >: A]: Wiring[AA] = Wiring.wire(a)
    def singleton[AA >: A]: Wiring[AA] = Wiring.singleton(a)
  }

}
