package org.basnal.scala

import scala.util.Random

object MemoryOptimizedStringProcessing {
  def main(args: Array[String]): Unit = {
    val numStrings = 100_000 // Large number of strings
    val stringLength = 100 // Fixed length
    val random = new Random()

    val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    // Preallocate buffer for each generated string
    val stringBuilder = new StringBuilder(stringLength)

    // Generate and process strings efficiently
    val processedResults = new Array[String](numStrings)

    for (i <- 0 until numStrings) {
      stringBuilder.clear() // Reuse buffer instead of creating new objects
      for (_ <- 0 until stringLength) {
        stringBuilder.append(charPool.charAt(random.nextInt(charPool.length)))
      }
      processedResults(i) = processString(stringBuilder.toString())
    }

    // Print sample output
    println(s"Processed ${processedResults.length} strings")
  }

  // Optimized processing using immutable transformations
  def processString(input: String): String = {
    input.replaceAll("[AEIOUaeiou]", "").toUpperCase() // Remove vowels & convert to uppercase
  }
}
