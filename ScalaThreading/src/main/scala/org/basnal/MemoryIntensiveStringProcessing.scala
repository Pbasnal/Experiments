package org.basnal.scala

import scala.util.Random
import java.security.MessageDigest

object MemoryIntensiveStringProcessing {
  def main(args: Array[String]): Unit = {
    val numStrings = 500_000 // Large number of strings
    val stringLength = 20

    // Generate a large list of random strings
    val randomStrings = (1 to numStrings).map(_ => generateRandomString(stringLength))

    println("Generated strings, starting processing...")

    val transformedStrings = randomStrings.map { str =>
      val upper = str.toUpperCase // Creates a new string
      val reversed = upper.reverse // Another new string
      val hashed = hashString(reversed) // Another new string
      hashed // Store it in the final list
    }

    println(s"Processing completed. Generated ${transformedStrings.size} transformed strings.")
  }

  // Generate a random string of given length
  def generateRandomString(length: Int): String = {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    (1 to length).map(_ => chars(Random.nextInt(chars.length))).mkString
  }

  // Compute hash of a string (produces a new string object)
  def hashString(input: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.getBytes("UTF-8"))
    hashBytes.map("%02x".format(_)).mkString
  }
}
