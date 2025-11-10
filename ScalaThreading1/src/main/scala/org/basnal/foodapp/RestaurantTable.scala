package org.basnal.foodapp

/** Columnar representation of restaurants – links to AreaTable via index. */
final case class RestaurantTable(
  ids      : Vector[RestaurantId],
  areaIdx  : Vector[Int],      // index into AreaTable
  isOpen   : Vector[Boolean]
) {
  def size: Int = ids.size
} 