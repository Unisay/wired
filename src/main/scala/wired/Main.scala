package wired

import java.util.concurrent.Executor

import cats.Eval
import cats.implicits._
import wired.wiring._

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

  def wiredComponentD: ComponentD Requires String =
    SomeD.wire(ask[String])

  def wiredComponentC: ComponentC Requires ConfigComponentC =
    SomeC.wire[ConfigComponentC](ask.map(_.name), wiredEnv)

  def wiredComponentB: ComponentB Requires ConfigComponentB =
    SomeB.wire[ConfigComponentB](ask.map(_.name), wiredComponentC.contramap(_.c), wiredEnv)

  def wiredComponentA: ComponentA Requires ConfigComponentA =
    SomeA.wire[ConfigComponentA](ask.map(_.name), wiredComponentB.contramap(_.b), wiredEnv)

  def wiredApplication: Application Requires ConfigApplication =
    Application.wire[ConfigApplication](wiredComponentA.contramap(_.a), wiredComponentD.contramap(_.nameD))

}

object DefaultModule extends DefaultModule

/** Here dependant wirings are provided implicitly */
trait ImplicitModule {

  type WiredD = ComponentD Requires String
  type WiredC = ComponentC Requires ConfigComponentC
  type WiredB = ComponentB Requires ConfigComponentB
  type WiredA = ComponentA Requires ConfigComponentA
  type WiredApplication = Application Requires ConfigApplication

  implicit def wiredEnv: Wired[Env] =
    Env(scala.concurrent.ExecutionContext.global).wire.singleton

  implicit def wiredComponentD: WiredD = SomeD.wire(ask[String])

  implicit def wiredComponentC(implicit env: Wired[Env]): WiredC =
    SomeC.wire[ConfigComponentC](ask.map(_.name), env)

  implicit def wiredComponentB(implicit c: WiredC, env: Wired[Env]): WiredB =
    SomeB.wire[ConfigComponentB](ask.map(_.name), c.contramap(_.c), env)

  implicit def wiredComponentA(implicit b: WiredB, env: Wired[Env]): WiredA =
    SomeA.wire[ConfigComponentA](ask.map(_.name), b.contramap(_.b), env)

  implicit def wiredApplication(implicit a: WiredA, d: WiredD): WiredApplication =
    Application.wire[ConfigApplication](a.contramap(_.a), d.contramap(_.nameD))
}

object ImplicitModule extends ImplicitModule

trait TestModule extends DefaultModule {
  private val syncExecutor = ExecutionContext.fromExecutor(new Executor { def execute(r: Runnable): Unit = r.run() })
  override val wiredEnv = Env(syncExecutor).wire.singleton
}

object TestModule extends TestModule

object Main {

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


