package org.basnal.scala

import scala.annotation.tailrec
import scala.util.Random

object CPUBottleneckExample {
  def main(args: Array[String]): Unit = {
    val size = 10_000_000 // Large array
    val arr = Array.fill(size)(Random.nextInt(100)) // Random integers between 0 and 99

    val numQueries = 10_000_0 // Number of queries
    val random = new Random()

    val queries = (1 to numQueries).map { _ =>
      if (random.nextDouble() < 0.1) { // 10% probability for full range query
        (0, size - 1)
      } else {
        val l = random.nextInt(size)
        val r = l + random.nextInt((size - l) min 10000) // Ensure r is within bounds
        (l, r)
      }
    }

    val startTime = System.nanoTime()

    println("Started summations")

    @tailrec
    def resolveQueries(queries: IndexedSeq[(Int, Int)], i: Int, acc: IndexedSeq[Long] = IndexedSeq.empty): IndexedSeq[Long] = {
      queries.headOption match {
        case None => acc
        case Some((l, r)) =>
          val sum = rangeSum(arr, l, r)
          if (i % 1000 == 0) {
            println(s"Summation of $i elements done")
          }
          // Tail-recursive call with updated accumulator
          resolveQueries(queries.tail, i + 1, acc :+ sum)
      }
    }

    val sums = resolveQueries(queries, 0)

    val endTime = System.nanoTime()
    println(s"Total execution time: ${(endTime - startTime) / 1e6} ms")
    println(s"Total number of summations: ${sums.size}")
  }

  // **Brute-force sum calculation**
  def rangeSum(arr: Array[Int], l: Int, r: Int): Long = {
    var sum = 0L
    for (i <- l to r) {
      sum += arr(i)
    }
    sum
  }
}
