package org.basnal.scala

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Random
import java.util.concurrent.Executors

object ProfilerApp extends App {
  // Custom thread pool for better profiling control
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  // Simulates CPU-intensive work
  def cpuIntensiveTask(id: Int): Int = {
    val result = (1 to 1000000).map(_ => Random.nextInt()).sum
    println(s"Task $id completed on thread: " + Thread.currentThread().getName)
    result
  }

  // Simulates Memory-intensive work
  def memoryIntensiveTask(id: Int): Array[Int] = {
    val array = Array.fill(1000000)(Random.nextInt())
    println(s"Memory Task $id completed on thread: " + Thread.currentThread().getName)
    array
  }

  // Create multiple parallel tasks
  val futures = (1 to 10).map { i =>
    Future {
      if (i % 2 == 0) cpuIntensiveTask(i) else memoryIntensiveTask(i)
    }
  }

  // Wait for all tasks to complete
  Await.result(Future.sequence(futures), 5.minutes)

  println("All tasks completed. Press Enter to exit.")
  scala.io.StdIn.readLine()
}
