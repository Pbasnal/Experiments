package org.basnal.foodapp

import cats.effect.IO
import cats.syntax.all._

object RestaurantCloseWorkflow {
  final case class Request(restaurantIdx: Int)
  final case class State(
    restaurantIdx: Int,
    restaurantTable: Option[RestaurantTable] = None,
    itemTable: Option[ItemTable] = None
  )

  private val fetchTables: Pipeline[IO, State, State] =
    Pipeline { st =>
      (
        Database.fetchRestaurantTable,
        Database.fetchItemTable
      ).parMapN { (rest, item) =>
        st.copy(
          restaurantTable = Some(rest),
          itemTable = Some(item)
        )
      }
    }

  private val closeRestaurant: Pipeline[IO, State, State] =
    Pipeline { st =>
      st.restaurantTable match {
        case Some(table) =>
          IO.pure(st.copy(restaurantTable = Some(RestaurantTableOps.markRestaurantClosed(table, st.restaurantIdx))))
        case None =>
          IO.raiseError(new IllegalStateException("Restaurant table not loaded"))
      }
    }

  private val closeItems: Pipeline[IO, State, State] =
    Pipeline { st =>
      st.itemTable match {
        case Some(table) =>
          IO.pure(st.copy(itemTable = Some(ItemTableOps.markByRestaurantIdx(table, st.restaurantIdx, value = false))))
        case None =>
          IO.raiseError(new IllegalStateException("Item table not loaded"))
      }
    }

  private val saveTables: Pipeline[IO, State, Unit] =
    Pipeline { st =>
      (
        st.restaurantTable.traverse_(Database.saveRestaurantTable),
        st.itemTable.traverse_(Database.saveItemTable)
      ).parMapN[Unit] { case (_, _) => () }
    }

  val pipeline: Pipeline[IO, State, Unit] =
    PipelineBuilder(fetchTables)
      .step(closeRestaurant)
      .step(closeItems)
      .step(saveTables)
      .build

  def run(req: Request): IO[Unit] = pipeline.run(State(req.restaurantIdx))
} 