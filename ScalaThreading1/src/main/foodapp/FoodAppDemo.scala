package org.basnal.scala.foodapp

import cats.effect.{IO, IOApp}
import java.util.UUID

/** Simple demonstration of the three data-oriented workflows.
  *
  * Steps:
  * 1. Seed an initial in-memory ItemTable.
  * 2. Mark one item as out-of-stock.
  * 3. Close an entire restaurant.
  * 4. Close an area.
  *
  * After each step we print a compact view of the table so you can trace the transformation.
  */
object FoodAppDemo extends IOApp.Simple {
  // ----- Helper IDs -----
  private def newItemId(): ItemId           = ItemId(UUID.randomUUID())
  private val areaA: AreaId                 = AreaId(UUID.randomUUID())
  private val areaB: AreaId                 = AreaId(UUID.randomUUID())
  private val rest1: RestaurantId           = RestaurantId(UUID.randomUUID())
  private val rest2: RestaurantId           = RestaurantId(UUID.randomUUID())

  private val item1 = newItemId()
  private val item2 = newItemId()
  private val item3 = newItemId()
  private val item4 = newItemId()

  // ----- Seed data -----
  // --- Area & Restaurant tables -------------------------------------------
  private val areaTable = AreaTable(
    ids    = Vector(areaA, areaB),
    isOpen = Vector(true, true)
  )

  private val restaurantTable = RestaurantTable(
    ids     = Vector(rest1, rest2),
    areaIdx = Vector(0, 1),   // rest1 in areaA, rest2 in areaB
    isOpen  = Vector(true, true)
  )

  private val initialItemTable = ItemTable(
    ids           = Vector(item1, item2, item3, item4),
    restaurantIdx = Vector(0, 0, 1, 1), // matches restaurantTable rows
    areaIdx       = Vector(0, 0, 1, 1), // directly cached for fast lookup
    inStock       = Vector(true, true, true, true),
    priceCents    = Vector(1000, 1200, 900, 1100)
  )

  // Pretty-printer -----------------------------------------------------------
  private def printTable(label: String): IO[Unit] =
    Database.fetchItemTable.flatMap { tbl =>
      val rows = tbl.ids.indices.map { i =>
        val idFrag  = tbl.ids(i).value.toString.take(4)
        val restFrag = restaurantTable.ids(tbl.restaurantIdx(i)).value.toString.take(4)
        val areaFrag = areaTable.ids(tbl.areaIdx(i)).value.toString.take(4)
        s"item $idFrag | rest $restFrag | area $areaFrag | inStock = ${tbl.inStock(i)}"
      }.mkString("\n")
      IO.println(s"""
                 |--- $label ---
                 |$rows
                 |--------------------------------------------""".stripMargin)
    }

  // Main program -------------------------------------------------------------
  override def run: IO[Unit] = for {
    _ <- IO.println("Seeding initial data").*>
         (Database.saveAreaTable(areaTable) *> Database.saveRestaurantTable(restaurantTable) *> Database.saveItemTable(initialItemTable))
    _ <- printTable("Initial state")

    // 1️⃣  Item out-of-stock --------------------------------------------------
    _ <- IO.println("\n➡️  Marking item2 out-of-stock…")
    _ <- ItemOOSWorkflow.run(ItemOOSWorkflow.Request(Vector(item2)))
    _ <- printTable("After item OOS")

    // 2️⃣  Restaurant closed --------------------------------------------------
    _ <- IO.println("\n➡️  Closing restaurant rest2…")
    _ <- RestaurantCloseWorkflow.run(RestaurantCloseWorkflow.Request(1))
    _ <- printTable("After restaurant closed")

    // 3️⃣  Area closed --------------------------------------------------------
    _ <- IO.println("\n➡️  Closing areaA…")
    _ <- AreaUnavailableWorkflow.run(AreaUnavailableWorkflow.Request(0))
    _ <- printTable("After area closed")
  } yield ()
} 