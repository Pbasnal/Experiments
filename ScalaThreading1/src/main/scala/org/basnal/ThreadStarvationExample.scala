package org.basnal.scala

import java.util.concurrent.{Executors, ThreadFactory}

object ThreadStarvationExample {
  def main(args: Array[String]): Unit = {
    val threadFactory = new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val thread = new Thread(r)
        thread.setPriority(Thread.MAX_PRIORITY) // Set high priority
        thread
      }
    }

    // Thread pool with only 2 threads (small size)
    val executor = Executors.newFixedThreadPool(2, threadFactory)

    // Long-running high-priority tasks
    for (i <- 1 to 2) {
      executor.execute(() => {
        println(s"High-Priority Task $i running")
        while (true) {
          Thread.sleep(10)
        } // Infinite loop prevents other tasks from executing
      })
    }

    // Low-priority task (never gets executed)
    executor.execute(() => println("Low-priority task executed!"))

    executor.shutdown()
  }
}

