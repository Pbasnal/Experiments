package org.basnal.foodapp

import cats.effect.IO

object ItemOOSWorkflow {
  final case class Request(outOfStockIds: Vector[ItemId])
  final case class State(inStockIds: Vector[ItemId], outOfStockIds: Vector[ItemId])

  private val fetch: Pipeline[IO, State, (State, ItemTable)] =
    Pipeline(state => Database.fetchItemTable.map(tbl => (state, tbl)))

  private val mutate: Pipeline[IO, (State, ItemTable), ItemTable] =
    Pipeline { case (state, tbl) =>
      val map = state.outOfStockIds.map(_ -> false).toMap
      IO.pure(ItemTableOps.updateInStockFlags(tbl, map))
    }

  private val save: Pipeline[IO, ItemTable, Unit] =
    Pipeline(Database.saveItemTable)

  val pipeline: Pipeline[IO, State, Unit] =
    PipelineBuilder(fetch).step(mutate).step(save).build

  def run(req: Request): IO[Unit] = pipeline.run(State(Vector.empty, req.outOfStockIds))
} 