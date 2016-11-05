package di

import java.util.concurrent.Executor

import cats.Eval
import cats.syntax.cartesian._
import di.Wiring._

import scala.concurrent.ExecutionContext


/**
  * Example of a dependency-injection approach in scala
  *
  * Requirements:
  * ☑ In any place of a program be able to wire a sub-component
  * ☑ Readable compile-time error messages
  * ☑ Handle alternative wirings (for testing)
  * ☐ Handle cyclic-dependencies
  */


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
  def wiredEnv = Env(scala.concurrent.ExecutionContext.global).singleton
  def wiredComponentC(config: ConfigComponentC): Wiring[ComponentC] = config.name.wire |@| wiredEnv map SomeC
  def wiredComponentB(config: ConfigComponentB) = config.name.wire |@| wiredComponentC(config.c) |@| wiredEnv map SomeB
  def wiredComponentA(config: ConfigComponentA) = config.name.wire |@| wiredComponentB(config.b) |@| wiredEnv map SomeA
  def wiredApplication(config: ConfigApplication) = Application of wiredComponentA(config.a)
}

object DefaultModule extends DefaultModule

trait TestModule extends DefaultModule {
  override val wiredEnv = {
    val syncExecutor = new Executor { def execute(r: Runnable): Unit = r.run() }
    Env of ExecutionContext.fromExecutor(syncExecutor).singleton
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
    val testModule = new TestModule { override def wiredComponentC(c: ConfigComponentC) = MockC.wire[ComponentC] }
    val application: Application = testModule.wiredApplication(readConfig).value
    println(application.run.value)
  }

  private def readConfig: ConfigApplication =
    ConfigApplication(ConfigComponentA(name = "A", ConfigComponentB(name = "B", ConfigComponentC(name = "C"))))

}


