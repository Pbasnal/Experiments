package org.basnal.foodapp

object ItemTableOps {

  /** Generic flag update by index collection. */
  private def flip(table: ItemTable, affected: Vector[Int], value: Boolean): ItemTable = {
    val newFlags = table.inStock.indices.map { idx =>
      if (affected.contains(idx)) value else table.inStock(idx)
    }.toVector
    table.copy(inStock = newFlags)
  }

  /** Update arbitrary item→flag map (used for Item OOS workflow). */
  def updateInStockFlags(table: ItemTable, updates: Map[ItemId, Boolean]): ItemTable = {
    val newFlags = table.ids.zipWithIndex.map { case (id, idx) =>
      updates.getOrElse(id, table.inStock(idx))
    }
    table.copy(inStock = newFlags)
  }

  /** Mark every item belonging to the given restaurant row as `value`. */
  def markByRestaurantIdx(table: ItemTable, restIdx: Int, value: Boolean): ItemTable = {
    val affected = table.restaurantIdx.zipWithIndex.collect { case (idx, i) if idx == restIdx => i }.toVector
    flip(table, affected, value)
  }

  /** Mark every item located in the given area row as `value`. */
  def markByAreaIdx(table: ItemTable, areaIdx: Int, value: Boolean): ItemTable = {
    val affected = table.areaIdx.zipWithIndex.collect { case (idx, i) if idx == areaIdx => i }.toVector
    flip(table, affected, value)
  }
} 