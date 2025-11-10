package org.basnal.scala.comix

import org.basnal.scala.comix.Domain.{ArtistId, ComicId, Genre, Language}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object DataDrivenWorkflow extends App {

  case class ComicData(
                        ids: Vector[ComicId],
                        titles: Vector[String],
                        artistIds: Vector[ArtistId],
                        genres: Vector[List[Genre]],
                        languages: Vector[Language],
                        createdAts: Vector[Instant]
                      ) {

    def printAllComics(): Unit = {
      println("Printing All")
      for (i <- ids.indices) {
        println(s"${titles(i)} => ${languages(i)}")
      }

    }

  }


  case class StateMutation[S, A](run: S => (A, S)) {
    def map[B](f: A => B): StateMutation[S, B] = {
      StateMutation(s => {
        val (a, state) = run(s)
        (f(a), state)
      })
    }

    def flatMap[B](f: A => StateMutation[S, B]): StateMutation[S, B] =
      StateMutation(s => {
        val (a, intermediateState) = run(s)
        f(a).run(intermediateState)
      })
  }

  type ComicState[A] = StateMutation[ComicData, A]

  def extractMetadata(): ComicState[Unit] =
    StateMutation(comicData => {
      for (i <- comicData.ids.indices) {
        println(s"${comicData.titles(i)} => ${comicData.languages(i)}")
      }

      ((), comicData)
    })

  def uploadComic(): ComicState[Future[Unit]] =
    StateMutation(comicData => {
      val future = Future {
        for (i <- comicData.ids.indices) {
          println(s"Starting upload for ${comicData.titles(i)} => ${comicData.languages(i)}")
        }
        ()
      }
      (future, comicData)
    })

  def illegalOverride(): ComicState[Future[Vector[String]]] =
    StateMutation(comicData => {

      val future = Future[Vector[String]] {
        val newTitles = comicData.titles.map(_ + " bungo")
        (newTitles)
      }
      val newTitles = Await.result(future, 5.seconds)

      (future, comicData.copy(titles = newTitles))
    })

  def validateUploadedChapter(future: Future[Unit]): ComicState[Unit] = StateMutation(comicData => {
    Await.result(future, 5.seconds)
    (future, comicData)
  })

  val comicPipeline = for {
    _ <- extractMetadata()
    uploadFut <- uploadComic()
    _ <- illegalOverride()
    _ <- validateUploadedChapter(uploadFut)
  } yield ()


  val initialState = ComicData(
    ids = Vector(ComicId(UUID.randomUUID())),
    titles = Vector("New Comic"),
    artistIds = Vector(ArtistId(UUID.randomUUID())),
    genres = Vector(List(Genre("Fantasy"), Genre("Drama"))),
    languages = Vector(Language("English")),
    createdAts = Vector(Instant.now())
  )

  val (result, finalState) = comicPipeline.run(initialState)

  finalState.printAllComics()

}
