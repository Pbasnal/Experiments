package org.basnal.scala

import scala.util.Random

object MemoryOptimisedCpuBottleneck {
  def main(args: Array[String]): Unit = {
    val size = 10_000_000 // Large array
    val arr = Array.fill(size)(Random.nextInt(100)) // Random integers between 0 and 99

    val numQueries = 100_000 // Reduced number of queries for demonstration
    val random = new Random()

    // Object pool to reuse query objects
    val queryPool = new Array[(Int, Int)](numQueries)

    for (i <- queryPool.indices) {
      if (random.nextDouble() < 0.1) { // 10% probability for full range query
        queryPool(i) = (0, size - 1)
      } else {
        val l = random.nextInt(size)
        val r = l + random.nextInt((size - l) min 10000) // Ensure r is within bounds
        queryPool(i) = (l, r)
      }
    }

    val startTime = System.nanoTime()

    println("Started summations")

    val sums = new Array[Long](numQueries) // Preallocate results array

    // Processing queries using optimized sum calculation
    for (i <- queryPool.indices) {
      val (l, r) = queryPool(i)
      sums(i) = rangeSum(arr, l, r)
      if (i % 1000 == 0) {
        println(s"Summation of $i elements done")
      }
    }

    val endTime = System.nanoTime()
    println(s"Total execution time: ${(endTime - startTime) / 1e6} ms")
    println(s"Total number of summations: ${sums.length}")
  }

  // **Optimized Sum Calculation using Prefix Sum**
  def rangeSum(arr: Array[Int], l: Int, r: Int): Long = {
    val prefixSum = new Array[Long](arr.length + 1)
    for (i <- arr.indices) {
      prefixSum(i + 1) = prefixSum(i) + arr(i)
    }
    prefixSum(r + 1) - prefixSum(l)
  }
}

