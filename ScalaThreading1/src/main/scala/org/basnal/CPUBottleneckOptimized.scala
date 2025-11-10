package org.basnal.scala

package org.basnal.scala

import scala.util.Random

object CPUBottleneckOptimized {
  def main(args: Array[String]): Unit = {
    val size = 10_000_000 // Large array
    val arr = Array.fill(size)(Random.nextInt(100)) // Random integers between 0 and 99

    val segmentTree = new SegmentTree(arr) // Build segment tree

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

    val sums = queries.zipWithIndex.map { case ((l, r), i) =>
      val sum = segmentTree.query(l, r)
      if (i % 1000 == 0) println(s"Summation of $i elements done")
      sum
    }

    val endTime = System.nanoTime()
    println(s"Total execution time: ${(endTime - startTime) / 1e6} ms")
    println(s"Total number of summations: ${sums.size}")
  }
}

// **Segment Tree Implementation**
class SegmentTree(arr: Array[Int]) {
  private val n = arr.length
  private val tree = Array.ofDim[Long](4 * n) // Segment tree storage

  // Build segment tree
  private def build(node: Int, start: Int, end: Int): Unit = {
    if (start == end) {
      tree(node) = arr(start)
    } else {
      val mid = (start + end) / 2
      build(2 * node + 1, start, mid)
      build(2 * node + 2, mid + 1, end)
      tree(node) = tree(2 * node + 1) + tree(2 * node + 2)
    }
  }

  // Query sum in range [l, r]
  private def query(node: Int, start: Int, end: Int, l: Int, r: Int): Long = {
    if (r < start || l > end) return 0 // No overlap
    if (l <= start && r >= end) return tree(node) // Total overlap

    val mid = (start + end) / 2
    query(2 * node + 1, start, mid, l, r) +
      query(2 * node + 2, mid + 1, end, l, r)
  }

  // Public API
  def query(l: Int, r: Int): Long = query(0, 0, n - 1, l, r)

  // Initialize segment tree
  build(0, 0, n - 1)
}

