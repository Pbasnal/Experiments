package org.basnal.foodapp.oo

import java.util.UUID

case class ItemId(value: UUID)
case class RestaurantId(value: UUID)
case class AreaId(value: UUID)

// OO Models with proper relationships
case class Item(
  id: ItemId,
  restaurantId: RestaurantId,  // Reference by ID instead of object to avoid circular reference
  inStock: Boolean,
  priceCents: Int
)

case class Restaurant(
  id: RestaurantId,
  areaId: AreaId,  // Reference by ID instead of object to avoid circular reference
  isOpen: Boolean,
  itemIds: List[ItemId] = List.empty  // Store item IDs instead of objects
) {
  def addItemId(itemId: ItemId): Restaurant = copy(itemIds = itemId :: itemIds)
  def markClosed(): Restaurant = copy(isOpen = false)
}

case class Area(
  id: AreaId,
  isOpen: Boolean,
  restaurantIds: List[RestaurantId] = List.empty  // Store restaurant IDs instead of objects
) {
  def addRestaurantId(restaurantId: RestaurantId): Area = copy(restaurantIds = restaurantId :: restaurantIds)
  def markClosed(): Area = copy(isOpen = false)
} 