package org.basnal.foodapp

/** Columnar representation of geographical areas. */
final case class AreaTable(
  ids    : Vector[AreaId],
  isOpen : Vector[Boolean]
) {
  def size: Int = ids.size
} 