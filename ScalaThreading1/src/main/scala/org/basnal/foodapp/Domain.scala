package org.basnal.foodapp

import java.util.UUID

/** Identifier wrappers ensure type safety across the data-oriented tables. */
final case class ItemId(value: UUID)       extends AnyVal
final case class RestaurantId(value: UUID) extends AnyVal
final case class AreaId(value: UUID)       extends AnyVal 