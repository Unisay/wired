package di

import java.util.concurrent.Executor

import cats.Eval
import cats.implicits._
import di.wiring._

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

trait DefaultModule {

  def wiredEnv: Wired[Env] =
    Env(scala.concurrent.ExecutionContext.global).wire.singleton

  def wiredComponentD: ComponentD Requires String =
    SomeD.wire(ask[String]).widen

  def wiredComponentC: ComponentC Requires ConfigComponentC =
    SomeC.wire[ConfigComponentC](ask.map(_.name), wiredEnv.ignoring).widen

  def wiredComponentB: ComponentB Requires ConfigComponentB =
    SomeB.wire[ConfigComponentB](ask.map(_.name), wiredComponentC.contramap(_.c), wiredEnv.ignoring).widen

  def wiredComponentA: ComponentA Requires ConfigComponentA =
    SomeA.wire[ConfigComponentA](ask.map(_.name), wiredComponentB.contramap(_.b), wiredEnv.ignoring).widen

  def wiredApplication: Application Requires ConfigApplication =
    Application.wire[ConfigApplication](wiredComponentA.contramap(_.a), wiredComponentD.contramap(_.nameD))

}

object DefaultModule extends DefaultModule

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

    val application: Application = wiredApplication(readConfig).value
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
    val application: Application = testModule.wiredApplication(readConfig).value
    println(application.run.value)
  }

  private def readConfig: ConfigApplication =
    ConfigApplication(
      a = ConfigComponentA(name = "A",
        b = ConfigComponentB(name = "B",
          c = ConfigComponentC(name = "C"))),
      nameD = "D")

}


