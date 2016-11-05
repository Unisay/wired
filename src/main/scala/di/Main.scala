package di

import java.util.concurrent.Executor

import cats.Eval
import cats.implicits._
import di.Wiring._

import scala.concurrent.ExecutionContext

case class Application(a: ComponentA) {
  def run: Eval[String] = Eval.later(a.toString)
}

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

case class Env(ec: ExecutionContext)
case class SomeA(name: String, b: ComponentB, env: Env) extends ComponentA
case class SomeB(name: String, c: ComponentC, env: Env) extends ComponentB
case class SomeC(name: String, env: Env) extends ComponentC

case class ConfigComponentA(name: String, b: ConfigComponentB)
case class ConfigComponentB(name: String, c: ConfigComponentC)
case class ConfigComponentC(name: String)
case class ConfigApplication(a: ConfigComponentA)

trait DefaultModule {
  def wiredEnv: Wired[Env] = Env(scala.concurrent.ExecutionContext.global).singleton

  def wiredComponentC: ComponentC Requires ConfigComponentC =
    ask[ConfigComponentC].map(_.name) |@| wiredEnv.ignoring map SomeC

  def wiredComponentB: ComponentB Requires ConfigComponentB =
    ask[ConfigComponentB].map(_.name) |@| wiredComponentC.contramap(_.c) |@| wiredEnv.ignoring map SomeB

  def wiredComponentA: ComponentA Requires ConfigComponentA =
    ask[ConfigComponentA].map(_.name) |@| wiredComponentB.contramap(_.b) |@| wiredEnv.ignoring map SomeA

  def wiredApplication: Application Requires ConfigApplication =
    wiredComponentA.contramap((_: ConfigApplication).a) map Application

}

object DefaultModule extends DefaultModule

trait TestModule extends DefaultModule {
  override val wiredEnv = {
    val syncExecutor = new Executor { def execute(r: Runnable): Unit = r.run() }
    ExecutionContext.fromExecutor(syncExecutor).singleton map Env
  }
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
      override def wiredComponentC = MockC.prototype[ComponentC].ignoring[ConfigComponentC]
    }
    val application: Application = testModule.wiredApplication(readConfig).value
    println(application.run.value)
  }

  private def readConfig: ConfigApplication =
    ConfigApplication(ConfigComponentA(name = "A", ConfigComponentB(name = "B", ConfigComponentC(name = "C"))))

}


