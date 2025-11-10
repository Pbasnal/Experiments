package org.basnal.oom

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import java.text.NumberFormat
import java.util.Locale

// exit on oom
// sbt 'set javaOptions += "-Xmx512M"' 'set javaOptions += "-XX:+ExitOnOutOfMemoryError"' "runMain org.basnal.oom.MemoryExhaustionDemo"
//
// handle oom
// sbt 'set javaOptions += "-Xmx512M"' "runMain org.basnal.oom.MemoryExhaustionDemo"

object MemoryExhaustionDemo extends App {
  // Helper function to format memory sizes
  def formatBytes(bytes: Long): String = {
    val units = Array("B", "KB", "MB", "GB")
    var value = bytes.toDouble
    var unitIndex = 0
    
    while (value > 1024 && unitIndex < units.length - 1) {
      value /= 1024
      unitIndex += 1
    }
    
    f"$value%.2f ${units(unitIndex)}"
  }

  // Helper function to print memory stats
  def printMemoryStats(): Unit = {
    val runtime = Runtime.getRuntime
    val format = NumberFormat.getNumberInstance(Locale.US)
    
    val maxMemory = runtime.maxMemory
    val allocatedMemory = runtime.totalMemory
    val freeMemory = runtime.freeMemory
    val usedMemory = allocatedMemory - freeMemory
    
    println(s"""
      |=== Memory Statistics ===
      |Maximum memory: ${formatBytes(maxMemory)}
      |Allocated memory: ${formatBytes(allocatedMemory)}
      |Free memory: ${formatBytes(freeMemory)}
      |Used memory: ${formatBytes(usedMemory)}
      |======================""".stripMargin)
  }

  // Keep track of allocated arrays to prevent garbage collection
  val memoryLeakHolder = ArrayBuffer[Array[Byte]]()
  
  try {
    println("Starting memory exhaustion simulation...")
    printMemoryStats()
    
    var iteration = 1
    val random = new Random()
    
    while (true) {
      // Allocate arrays of random sizes between 100MB and 500MB
      val sizeInMB = 100 + random.nextInt(401)  // Random size between 100MB and 500MB
      val arraySize = sizeInMB * 1024 * 1024
      
      println(s"\nIteration $iteration: Allocating ${formatBytes(arraySize)} of memory")
      
      // Create a byte array and fill it with random data to ensure it's actually allocated
      val array = new Array[Byte](arraySize)
      random.nextBytes(array)
      
      // Keep a reference to prevent garbage collection
      memoryLeakHolder += array
      
      printMemoryStats()
      
      // Small delay to make the output readable
      Thread.sleep(100)
      
      iteration += 1
    }
  } catch {
    case oom: OutOfMemoryError =>
      println("\n!!! Out of Memory Error occurred !!!")
      println(s"Iterations completed before OOM: ${memoryLeakHolder.size}")
      printMemoryStats()
      
      // Try to free some memory to allow for clean program termination
      memoryLeakHolder.clear()
      System.gc()
      
      println("\nFinal memory stats after cleanup:")
      printMemoryStats()
  } finally {
    println("\nProgram completed.")
  }
}
