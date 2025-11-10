package org.basnal.scala

import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Main {
  def main(args: Array[String]): Unit = {
    println("Hello world!")
  }
}

object ThreadExample extends App {
  val thread = new Thread(() => {
    for (i <- 1 to 5) {
      println(s"Thread: $i")
      Thread.sleep(500)
    }
  })

  thread.start()
  for (i <- 1 to 5) {
    println(s"Main: $i")
    Thread.sleep(500)
  }
}

object FutureExample extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val threadMXBean = ManagementFactory.getThreadMXBean
  // future starts running when it gets created.
  val future = Future {
    for (i <- 1 to 5) {
      println(s"Future: $i")
      val thread = Thread.currentThread()
      val cpuTime = threadMXBean.getThreadCpuTime(thread.getId)
      println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
      println(s"Thread ID: ${thread.getId}, CPU Time: ${cpuTime / 1e6} ms")

      Thread.sleep(500)
    }
    42
  }

  // i want to print the thread Id, CPU core, system thread or logical thread
  // if possible, time taken for context switching
  future.onComplete {
    case scala.util.Success(value) =>
      println(s"Result: $value")
      val thread = Thread.currentThread()
      println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    case scala.util.Failure(exception) => println(s"Failed with: ${exception.getMessage}")
  }
  println("Main thread continues...")
  Thread.sleep(5000) // Keep the JVM alive for the future to complete
}

// future can run on thread 1 and onComplete callback on a different thread
object CustomExecutionContext extends App {
  val threadMXBean = ManagementFactory.getThreadMXBean
  val threadPoolExecutor = Executors.newFixedThreadPool(4)
   val executionContext = ExecutionContext.fromExecutor(threadPoolExecutor)

  val future = Future {
    val thread = Thread.currentThread()
    val cpuTime = threadMXBean.getThreadCpuTime(thread.getId)
    println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    println(s"Thread ID: ${thread.getId}, CPU Time: ${cpuTime / 1e6} ms")
  }(executionContext)
  future.onComplete {
    case scala.util.Success(value) =>
      println(s"Result: $value")
      val thread = Thread.currentThread()
      println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    case scala.util.Failure(exception) => println(s"Failed with: ${exception.getMessage}")
  }(executionContext)
  Thread.sleep(1000)

  threadPoolExecutor.shutdown()
}

// EC is prepared, then future and callbacks are executed on the same thread.
// but prepare method is deprecated
object PreparedExecutionContext extends App {
  val threadMXBean = ManagementFactory.getThreadMXBean
  val threadPoolExecutor = Executors.newFixedThreadPool(4)
  val preparedEc = ExecutionContext.global.prepare() // doesn't work with custom

  val future = Future {
    val thread = Thread.currentThread()
    val cpuTime = threadMXBean.getThreadCpuTime(thread.getId)
    println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    println(s"Thread ID: ${thread.getId}, CPU Time: ${cpuTime / 1e6} ms")
  }(preparedEc)
  future.onComplete {
    case scala.util.Success(value) =>
      println(s"Result: $value")
      val thread = Thread.currentThread()
      println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    case scala.util.Failure(exception) => println(s"Failed with: ${exception.getMessage}")
  }(preparedEc)
  Thread.sleep(1000)

  threadPoolExecutor.shutdown()
}

// handling future and callback in same thread using
// custom ec
object ParasiticExecutionContext extends App {
  val threadMXBean = ManagementFactory.getThreadMXBean
  val parasite = new ExecutionContext {
    override def execute(runnable: Runnable): Unit = runnable.run()

    override def reportFailure(cause: Throwable): Unit = println(s"Execution failed with $cause")
  }

  val future = Future {
    val thread = Thread.currentThread()
    Thread.sleep(100)
    val cpuTime = threadMXBean.getThreadCpuTime(thread.getId)
    println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    println(s"Thread ID: ${thread.getId}, CPU Time: ${cpuTime / 1e6} ms")
  }(ExecutionContext.global)

  future.onComplete {
    case scala.util.Success(value) =>
      println(s"Result: $value")
      val thread = Thread.currentThread()
      println(s"Thread ID: ${thread.getId}, Name: ${thread.getName}")
    case scala.util.Failure(exception) => println(s"Failed with: ${exception.getMessage}")
  }(parasite)


  Thread.sleep(1000)

}
