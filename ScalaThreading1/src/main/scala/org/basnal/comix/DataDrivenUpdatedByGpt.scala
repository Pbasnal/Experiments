package org.basnal.scala.comix

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

object DataDrivenUpdatedByGpt extends App {
  // Setup ExecutionContext for Futures
  implicit val ec: ExecutionContext = ExecutionContext.global

  // Domain Models
  case class ComicId(id: UUID)

  case class ArtistId(id: UUID)

  case class Genre(name: String)

  case class Language(name: String)

  case class ComicRow(
                       id: ComicId,
                       title: String,
                       artistId: ArtistId,
                       genres: List[Genre],
                       language: Language,
                       createdAt: Instant
                     )

  case class ComicData(rows: Vector[ComicRow]) {
    def printAllComics(): Unit = {
      println("--- All Comics ---")
      rows.foreach(row => println(s"${row.title} (${row.language.name})"))
    }
  }

  // State Monad
  case class StateMutation[S, A](run: S => (A, S)) {
    def map[B](f: A => B): StateMutation[S, B] =
      StateMutation(s => {
        val (a, newState) = run(s)
        (f(a), newState)
      })

    def flatMap[B](f: A => StateMutation[S, B]): StateMutation[S, B] =
      StateMutation(s => {
        val (a, intermediateState) = run(s)
        f(a).run(intermediateState)
      })
  }

  type ComicState[A] = StateMutation[ComicData, A]

  // Pipeline Stages (pure logic)
  def extractMetadata(): ComicState[Unit] = StateMutation { comicData =>
    comicData.rows.foreach(row => println(s"Metadata: ${row.title} [${row.language.name}]"))
    ((), comicData)
  }

  def computeNewTitles(): ComicState[Vector[String]] = StateMutation { comicData =>
    val newTitles = comicData.rows.map(_.title + " bungo")
    val updatedRows = comicData.rows.zip(newTitles).map {
      case (row, newTitle) => row.copy(title = newTitle)
    }
    (newTitles, comicData.copy(rows = updatedRows))
  }

  // Side-effect logic outside the pipeline
  def triggerUpload(state: ComicData): Future[Unit] = Future {
    println("Uploading comics...")
    state.rows.foreach(row => println(s"Uploading ${row.title} in ${row.language.name}"))
  }

  def validateUpload(fut: Future[Unit]): ComicState[Unit] = StateMutation { state =>
    Await.result(fut, 5.seconds)
    ((), state)
  }

  // Building the pipeline
  val comicPipeline: ComicState[Future[Unit]] = for {
    _ <- extractMetadata()
    _ <- computeNewTitles()
    uploadFut = triggerUpload(_: ComicData)
    fut <- StateMutation[ComicData, Future[Unit]](cd => (uploadFut(cd), cd))
    _ <- validateUpload(fut)
  } yield fut

  // Example initial data
  val initialState = ComicData(Vector(
    ComicRow(
      ComicId(UUID.randomUUID()),
      "New Comic",
      ArtistId(UUID.randomUUID()),
      List(Genre("Fantasy"), Genre("Drama")),
      Language("English"),
      Instant.now()
    )
  ))

  // Running the pipeline
  val (uploadFuture, finalState) = comicPipeline.run(initialState)
  Await.result(uploadFuture, 5.seconds)
  finalState.printAllComics()

}
