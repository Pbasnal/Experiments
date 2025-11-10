package org.basnal.foodapp

/** Columnar representation of menu items – rows link to `RestaurantTable` and `AreaTable` via their respective indices. */
final case class ItemTable(
  ids           : Vector[ItemId],
  restaurantIdx : Vector[Int],  // index into RestaurantTable
  areaIdx       : Vector[Int],  // index into AreaTable
  inStock       : Vector[Boolean],
  priceCents    : Vector[Int]
) {
  def size: Int = ids.size
}