package com.github.unisay.wired

import java.util.concurrent.Executor

import cats.Eval
import com.github.unisay.wired.all._

import scala.concurrent.ExecutionContext

/*

Component tree:                Config tree:

Application                    ConfigApplication
┃                              ┃
┣━━ ComponentA                 ┣━━ ConfigComponentA
┃     ┃                        ┃         ┃
┃     ┗━ ComponentB            ┃         ┗━ ConfigComponentB
┃          ┃                   ┃                  ┃
┃          ┗━ ComponentC       ┃                  ┗━ ConfigComponentC
┃                              ┃
┗━━ ComponentD                 ┗━━ String

*/

trait ComponentA {
  val name: String
  val b: ComponentB
}

trait ComponentB {
  val name: String
  val c: ComponentC
}

trait ComponentC {
  val name: String
}

trait ComponentD {
  val name: String
}

case class Application(a: ComponentA, d: ComponentD) { def run: Eval[String] = Eval.later(toString) }
case class Env(ec: ExecutionContext)
case class SomeA(name: String, b: ComponentB, env: Env) extends ComponentA
case class SomeB(name: String, c: ComponentC, env: Env) extends ComponentB
case class SomeC(name: String, env: Env) extends ComponentC
case class SomeD(name: String) extends ComponentD

case class ConfigComponentA(name: String, b: ConfigComponentB)
case class ConfigComponentB(name: String, c: ConfigComponentC)
case class ConfigComponentC(name: String)
case class ConfigApplication(a: ConfigComponentA, nameD: String)

/** Here dependant wirings are provided explicitly */
trait DefaultModule {

  def wiredEnv: Wired[Env] =
    Env(scala.concurrent.ExecutionContext.global).wire.singleton

  def wiredComponentD: String ->> ComponentD = ask[String] =>> SomeD

  def wiredComponentC: ConfigComponentC ->> ComponentC =
    SomeC.wire[ConfigComponentC](ask =>> (_.name), wiredEnv)

  def wiredComponentB: ConfigComponentB ->> ComponentB =
    SomeB.wire[ConfigComponentB](ask =>> (_.name), wiredComponentC <<= (_.c), wiredEnv)

  def wiredComponentA: ConfigComponentA ->> ComponentA =
    SomeA.wire[ConfigComponentA](ask =>> (_.name), wiredComponentB <<= (_.b), wiredEnv)

  def wiredApplication: ConfigApplication ->> Application =
    Application.wire[ConfigApplication](wiredComponentA <<= (_.a), wiredComponentD <<= (_.nameD))

}

object DefaultModule extends DefaultModule

/** Here dependant wirings are provided implicitly */
trait ImplicitModule {

  implicit def wiredEnv: Wired[Env] =
    Env(scala.concurrent.ExecutionContext.global).wire.singleton

  implicit def wiredComponentD: String ->> ComponentD = SomeD.wire(ask[String])

  implicit def wiredComponentC: ConfigComponentC ->> ComponentC =
    SomeC.wire[ConfigComponentC](ask =>> (_.name), wired0[Env])

  implicit def wiredComponentB(implicit c: ConfigComponentC ->> ComponentC): ConfigComponentB ->> ComponentB =
    SomeB.wire[ConfigComponentB](ask =>> (_.name), c <<= (_.c), wired0[Env])

  implicit def wiredComponentA: ConfigComponentA ->> ComponentA =
    SomeA.wire[ConfigComponentA](ask =>> (_.name), wired[ConfigComponentB, ComponentB] <<= (_.b), wired0[Env])

  implicit def wiredApplication: ConfigApplication ->> Application =
    Application.wire(
      wired[ConfigComponentA, ComponentA] <<= (_.a),
      wired[String, ComponentD] <<= (_.nameD)
    )
}

object ImplicitModule extends ImplicitModule

trait TestModule extends DefaultModule {
  private val syncExecutor = ExecutionContext.fromExecutor(new Executor { def execute(r: Runnable): Unit = r.run() })
  override val wiredEnv = Env(syncExecutor).wire.singleton
}

object TestModule extends TestModule

object Example {

  def main(args: Array[String]): Unit = {
    import DefaultModule._

    val application: Application = wiredApplication(readConfig).value
    println(application.run.value)

    testUsesTestModule()
    testUsesTestModuleWithAdHocOverrides()
  }

  def testUsesTestModule(): Unit = {
    import TestModule._

    val application: Application = wiredApplication.get(readConfig)
    println(application.run.value)
  }

  def testUsesTestModuleWithAdHocOverrides(): Unit = {
    object MockC extends ComponentC {
      val name: String = "Mock Component C"
      override def toString: String = name
    }
    val testModule = new TestModule {
      override def wiredComponentC = MockC.wire[ComponentC].ignoring[ConfigComponentC]
    }
    val application: Application = testModule.wiredApplication.get(readConfig)
    println(application.run.value)
  }

  private def readConfig: ConfigApplication =
    ConfigApplication(
      a = ConfigComponentA(name = "A",
        b = ConfigComponentB(name = "B",
          c = ConfigComponentC(name = "C"))),
      nameD = "D")

}


