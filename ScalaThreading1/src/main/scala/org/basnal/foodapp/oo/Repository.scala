package org.basnal.foodapp.oo

import cats.effect.IO
import scala.collection.concurrent.TrieMap

trait Repository[K, V] {
  def get(id: K): IO[Option[V]]
  def save(id: K, value: V): IO[Unit]
  def getAll: IO[List[V]]
}

class InMemoryRepository[K, V] extends Repository[K, V] {
  private val store = TrieMap.empty[K, V]

  override def get(id: K): IO[Option[V]] = IO.pure(store.get(id))
  override def save(id: K, value: V): IO[Unit] = IO.pure(store.update(id, value)).void
  override def getAll: IO[List[V]] = IO.pure(store.values.toList)
}

object Repository {
  def inMemory[K, V]: Repository[K, V] = new InMemoryRepository[K, V]
} 