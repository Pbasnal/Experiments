package org.basnal.foodapp

import cats.effect.IO
import cats.syntax.all._

object AreaUnavailableWorkflow {
  final case class Request(areaIdx: Int)
  final case class State(
    areaIdx: Int,
    areaTable: Option[AreaTable] = None,
    restaurantTable: Option[RestaurantTable] = None,
    itemTable: Option[ItemTable] = None
  )

  private val fetchTables: Pipeline[IO, State, State] =
    Pipeline { st =>
      (
        Database.fetchAreaTable,
        Database.fetchRestaurantTable,
        Database.fetchItemTable
      ).parMapN { (area, rest, item) =>
        st.copy(
          areaTable = Some(area),
          restaurantTable = Some(rest),
          itemTable = Some(item)
        )
      }
    }

  private val closeArea: Pipeline[IO, State, State] =
    Pipeline { st =>
      st.areaTable match {
        case Some(table) =>
          IO.pure(st.copy(areaTable = Some(AreaTableOps.markAreaClosed(table, st.areaIdx))))
        case None =>
          IO.raiseError(new IllegalStateException("Area table not loaded"))
      }
    }

  private val closeRestaurants: Pipeline[IO, State, State] =
    Pipeline { st =>
      st.restaurantTable match {
        case Some(table) =>
          IO.pure(st.copy(restaurantTable = Some(RestaurantTableOps.markRestaurantsByAreaClosed(table, st.areaIdx))))
        case None =>
          IO.raiseError(new IllegalStateException("Restaurant table not loaded"))
      }
    }

  private val closeItems: Pipeline[IO, State, State] =
    Pipeline { st =>
      st.itemTable match {
        case Some(table) =>
          IO.pure(st.copy(itemTable = Some(ItemTableOps.markByAreaIdx(table, st.areaIdx, value = false))))
        case None =>
          IO.raiseError(new IllegalStateException("Item table not loaded"))
      }
    }

  private val saveTables: Pipeline[IO, State, Unit] =
    Pipeline { st =>
      (
        st.areaTable.traverse_(Database.saveAreaTable),
        st.restaurantTable.traverse_(Database.saveRestaurantTable),
        st.itemTable.traverse_(Database.saveItemTable)
      ).parMapN[Unit] { case (_, _, _) => () }
    }

  val pipeline: Pipeline[IO, State, Unit] =
    PipelineBuilder(fetchTables)
      .step(closeArea)
      .step(closeRestaurants)
      .step(closeItems)
      .step(saveTables)
      .build

  def run(req: Request): IO[Unit] = pipeline.run(State(req.areaIdx))
} 