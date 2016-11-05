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

trait DefaultModule {
  def wiredEnv = Env(scala.concurrent.ExecutionContext.global).singleton
  def wiredComponentC: Wiring[ComponentC] = "Component C".wire |@| wiredEnv map SomeC
  def wiredComponentB: Wiring[ComponentB] = "Component B".wire |@| wiredComponentC |@| wiredEnv map SomeB
  def wiredComponentA: Wiring[ComponentA] = "Component A".wire |@| wiredComponentB |@| wiredEnv map SomeA
  def wiredApplication = Application of wiredComponentA
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

    val application: Application = wiredApplication.value
    println(application.run.value)

    testUsesTestModule()
    testUsesTestModuleWithAdHocOverrides()
  }

  def testUsesTestModule(): Unit = {
    import TestModule._

    val application: Application = wiredApplication.value
    println(application.run.value)
  }

  def testUsesTestModuleWithAdHocOverrides(): Unit = {
    object MockC extends ComponentC {
      val name: String = "Mock Component C"
      override def toString: String = name
    }
    val testModule = new TestModule { override val wiredComponentC = MockC.wire[ComponentC] }

    val application: Application = testModule.wiredApplication.value
    println(application.run.value)
  }

}


