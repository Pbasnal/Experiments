package org.basnal.scala.comix

import org.basnal.scala.comix.Domain.{Chapter, Comic, ComicId}

object Behaviours {
  def handleCommand(cmd: Command): List[Event] = ???
  def applyEvent(state: Chapter, event: Event): Chapter = ???

}

trait ComicRepository[F[_]] {
  def saveComic(comic: Comic): F[Unit]
  def findComic(id: ComicId): F[Option[Comic]]
}