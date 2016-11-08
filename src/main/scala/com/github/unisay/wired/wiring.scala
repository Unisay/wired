package com.github.unisay.wired

import cats._
import cats.data.Kleisli
import cats.implicits._

import scala.language.implicitConversions

trait Wirings {

  type Wiring[I, O] = Kleisli[Eval, I, O]
  type ->>[I, O] = Wiring[I, O]
  type <<-[O, I] = Wiring[I, O]
  type Wired[O] = Unit ->> O

  def ask[I]: I->>I = Kleisli.ask[Eval, I]

  def wire[I, O](value: O): Wired[O] = Kleisli.lift(Eval.now(value))

  implicit class WireSyntax[O](val value: O) {
    def wire[OO >: O]: Wired[OO] = Wirings.this.wire(value)
  }

  implicit class WiringSyntax[I, O](val wiring: I->>O) {
    def zip[R](right: I->>R): I->>(O,R) = wiring |@| right map (_ -> _)
    def =>>[R](f: O => R): I->>R = wiring map f
    def <<=[R](f: R => I): R->>O = wiring contramap f
    def singleton: I->>O = wiring.mapF(_.memoize)
    def get(i: I): O = wiring.run(i).value
  }

  implicit class IgnoringSyntax[O](val wired: Wired[O]) {
    def ignoring[I]: I->>O = wired.local(_ => ())
  }

  def wired0[T](implicit w: Wired[T]): Wired[T] = w

  def wired[I, O](implicit w: I->>O): I->>O = w

  implicit def ignoring[A, B](wired: Wired[A]): B->>A = wired.ignoring

  implicit def widening[A, B, D >: B](wiring: A->>B): A->>D = wiring.widen

  // Syntactic sugar:

  implicit class WireSyntaxF1[I1, O](val f: I1 => O) {
    def wire[I2 <: I1](wiring: I2->>I1): I2->>O = wiring map f
  }

  implicit class WireSyntaxF2[I1, I2, O](val f: (I1, I2) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2) = w1 |@| w2 map f
  }

  implicit class WireSyntaxF3[I1, I2, I3, O](val f: (I1, I2, I3) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3) = w1 |@| w2 |@| w3 map f
  }

  implicit class WireSyntaxF4[I1, I2, I3, I4, O](val f: (I1, I2, I3, I4) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4) = w1 |@| w2 |@| w3 |@| w4 map f
  }

  implicit class WireSyntaxF5[I1, I2, I3, I4, I5, O](val f: (I1, I2, I3, I4, I5) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4, w5: I->>I5) = w1 |@| w2 |@| w3 |@| w4 |@| w5 map f
  }

  implicit class WireSyntaxF6[I1, I2, I3, I4, I5, I6, O](val f: (I1, I2, I3, I4, I5, I6) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4, w5: I->>I5, w6: I->>I6) =
      w1 |@| w2 |@| w3 |@| w4 |@| w5 |@| w6 map f
  }

  implicit class WireSyntaxF7[I1, I2, I3, I4, I5, I6, I7, O](val f: (I1, I2, I3, I4, I5, I6, I7) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4, w5: I->>I5, w6: I->>I6, w7: I->>I7) =
      w1 |@| w2 |@| w3 |@| w4 |@| w5 |@| w6 |@| w7 map f
  }

  implicit class WireSyntaxF8[I1, I2, I3, I4, I5, I6, I7, I8, O](val f: (I1, I2, I3, I4, I5, I6, I7, I8) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4, w5: I->>I5, w6: I->>I6, w7: I->>I7, w8: I->>I8) =
      w1 |@| w2 |@| w3 |@| w4 |@| w5 |@| w6 |@| w7 |@| w8 map f
  }

  implicit class WireSyntaxF9[I1, I2, I3, I4, I5, I6, I7, I8, I9, O](val f: (I1, I2, I3, I4, I5, I6, I7, I8, I9) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4, w5: I->>I5, w6: I->>I6, w7: I->>I7, w8: I->>I8, w9: I->>I9) =
      w1 |@| w2 |@| w3 |@| w4 |@| w5 |@| w6 |@| w7 |@| w8 |@| w9 map f
  }

  implicit class WireSyntaxF10[I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, O](val f: (I1, I2, I3, I4, I5, I6, I7, I8, I9, I10) => O) {
    def wire[I](w1: I->>I1, w2: I->>I2, w3: I->>I3, w4: I->>I4, w5: I->>I5, w6: I->>I6, w7: I->>I7, w8: I->>I8, w9: I->>I9, w10: I->>I10) =
      w1 |@| w2 |@| w3 |@| w4 |@| w5 |@| w6 |@| w7 |@| w8 |@| w9 |@| w10 map f
  }

}

object all extends Wirings
