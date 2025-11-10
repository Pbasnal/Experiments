package org.basnal.foodapp

import cats.effect.IO

object Database {
  @volatile private var items      : Option[ItemTable]       = None
  @volatile private var restaurants: Option[RestaurantTable] = None
  @volatile private var areas      : Option[AreaTable]       = None

  // ----- Item table -----
  def fetchItemTable: IO[ItemTable] = IO(items.getOrElse(throw missing("ItemTable")))
  def saveItemTable(tbl: ItemTable): IO[Unit] = IO { items = Some(tbl) }

  // ----- Restaurant table -----
  def fetchRestaurantTable: IO[RestaurantTable] = IO(restaurants.getOrElse(throw missing("RestaurantTable")))
  def saveRestaurantTable(tbl: RestaurantTable): IO[Unit] = IO { restaurants = Some(tbl) }

  // ----- Area table -----
  def fetchAreaTable: IO[AreaTable] = IO(areas.getOrElse(throw missing("AreaTable")))
  def saveAreaTable(tbl: AreaTable): IO[Unit] = IO { areas = Some(tbl) }

  private def missing(name: String) = new IllegalStateException(s"$name not initialised")
} 