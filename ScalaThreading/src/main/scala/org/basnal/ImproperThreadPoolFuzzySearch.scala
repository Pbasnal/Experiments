package org.basnal.scala

import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}
import scala.collection.concurrent.TrieMap
import scala.util.Random


object ImproperThreadPoolFuzzySearch {

  // Generate a random string of given length
  def randomString(length: Int): String = {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    (1 to length).map(_ => chars(Random.nextInt(chars.length))).mkString
  }

  // Compute Levenshtein Distance (Brute Force Fuzzy Matching)
  def levenshteinDistance(s1: String, s2: String): Int = {
    val dp = Array.tabulate(s1.length + 1, s2.length + 1) { (i, j) =>
      if (i == 0) j else if (j == 0) i else 0
    }

    for (i <- 1 to s1.length; j <- 1 to s2.length) {
      dp(i)(j) = (dp(i - 1)(j) + 1)
        .min(dp(i)(j - 1) + 1)
        .min(dp(i - 1)(j - 1) + (if (s1(i - 1) == s2(j - 1)) 0 else 1))
    }

    dp(s1.length)(s2.length)
  }

  // Perform fuzzy search by finding the closest match for a given query
  def fuzzySearch(query: String, corpus: Seq[String]): String = {
    corpus.minBy(target => levenshteinDistance(query, target))
  }

  def main(args: Array[String]): Unit = {
    val numCorpusEntries = 500000 // Large dataset
    val numQueries = 500 // Number of search queries

    val startTime = System.nanoTime()
    // Generate a large corpus of random strings
    val corpus = (1 to numCorpusEntries).map(_ => randomString(10))

    // Generate random search queries
    val queries = (1 to numQueries).map(_ => randomString(8))

    // ThreadPool with only 2 threads (causes bottleneck)
    val queue = new LinkedBlockingQueue[Runnable]()
    val executor = new TimingThreadPool(25, 80, 60, TimeUnit.SECONDS, queue)
//     val executor = Executors.newFixedThreadPool(20)

    // Submit each query for processing in the thread pool
    for (query <- queries) {
      val myExec = executor.asInstanceOf[ThreadPoolExecutor]

      executor.execute(new Runnable {
        override def run(): Unit = {
          val result = fuzzySearch(query, corpus)
          println(s"Query: $query -> Best Match: $result (Thread: ${Thread.currentThread().getName})")
          println("Queue size: " + myExec.getQueue.size)
        }
      })
    }

    // Shutdown and wait for tasks to complete
    executor.shutdown()
    executor.awaitTermination(Long.MaxValue, TimeUnit.SECONDS)
    val endTime = System.nanoTime()
    println(s"Total execution time: ${(endTime - startTime) / 1e6} ms")
  }


  class TimingThreadPool(corePoolSize: Int, maxPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, queue: BlockingQueue[Runnable])
    extends ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, queue) {

    private val taskSubmissionTimes = TrieMap[Runnable, Long]()

    override def execute(command: Runnable): Unit = {
      val submissionTime = System.nanoTime()
      taskSubmissionTimes.put(command, submissionTime)
      super.execute(command)
    }

    override protected def beforeExecute(t: Thread, r: Runnable): Unit = {
      val startTime = System.nanoTime()
      taskSubmissionTimes.remove(r).foreach { submissionTime =>
        val waitTimeMs = (startTime - submissionTime) / 1_000_000
        println(s"Task waited for $waitTimeMs ms in the queue")
      }
      super.beforeExecute(t, r)
    }
  }

}

