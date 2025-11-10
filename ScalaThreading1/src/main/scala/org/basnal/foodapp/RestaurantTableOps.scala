package org.basnal.foodapp

object RestaurantTableOps {
  def markRestaurantClosed(table: RestaurantTable, restaurantIdx: Int): RestaurantTable = {
    require(restaurantIdx >= 0 && restaurantIdx < table.size, s"Invalid restaurant index: $restaurantIdx")
    
    table.copy(
      isOpen = table.isOpen.updated(restaurantIdx, false)
    )
  }

  def markRestaurantsByAreaClosed(table: RestaurantTable, areaIdx: Int): RestaurantTable = {
    val updatedIsOpen = table.isOpen.zipWithIndex.map { case (isOpen, idx) =>
      if (table.areaIdx(idx) == areaIdx) false else isOpen
    }
    
    table.copy(isOpen = updatedIsOpen)
  }
} 