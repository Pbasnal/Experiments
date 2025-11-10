package org.basnal.scala

import java.util.concurrent.locks.ReentrantLock

object DeadlockExample {
  val lock1 = new ReentrantLock()
  val lock2 = new ReentrantLock()

  def main(args: Array[String]): Unit = {
    val thread1 = new Thread(() => {
      lock1.lock()
      println("Thread 1 acquired Lock 1")
      Thread.sleep(100) // Simulate work

      lock2.lock()
      println("Thread 1 acquired Lock 2")

      lock2.unlock()
      lock1.unlock()
    })

    val thread2 = new Thread(() => {
      lock2.lock()
      println("Thread 2 acquired Lock 2")
      Thread.sleep(100) // Simulate work

      lock1.lock()
      println("Thread 2 acquired Lock 1")

      lock1.unlock()
      lock2.unlock()
    })

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()
  }
}
