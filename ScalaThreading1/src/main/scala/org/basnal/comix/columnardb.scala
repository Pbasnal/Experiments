package org.basnal.scala.comix

import org.basnal.scala.comix.Domain.{ArtistId, Comic, ComicId, Genre, Language}

import java.time.Instant
import java.time.temporal.ChronoUnit


object columnardb {
  implicit class PipeOps[A](private val a: A) extends AnyVal {
    def |>[B](f: A => B): B = f(a)
  }


  final case class ComicTable(
                               ids: Vector[ComicId],
                               titles: Vector[String],
                               artistIds: Vector[ArtistId],
                               genres: Vector[List[Genre]],
                               languages: Vector[Language],
                               createdAts: Vector[Instant]
                             ) {
    def size: Int = ids.size

    def insert(table: ComicTable, comic: Comic): ComicTable =
      ComicTable(
        table.ids :+ comic.id,
        table.titles :+ comic.title,
        table.artistIds :+ comic.artistId,
        table.genres :+ comic.genres,
        table.languages :+ comic.language,
        table.createdAts :+ comic.createdAt
      )

    def filterByGenre(table: ComicTable, genre: Genre): ComicTable = {
      val indices = table.genres.zipWithIndex.collect {
        case (gList, idx) if gList.contains(genre) => idx
      }

      projectByIndices(table, indices)
    }

    def filterByCreatedAfter(table: ComicTable, instant: Instant): ComicTable = {
      val indices = table.createdAts.zipWithIndex.collect {
        case (createdTime, idx) if createdTime.isAfter(instant) => idx
      }

      projectByIndices(table, indices)
    }

    def projectByIndices(table: ComicTable, indices: Vector[Int]): ComicTable =
      ComicTable(
        indices.map(table.ids),
        indices.map(table.titles),
        indices.map(table.artistIds),
        indices.map(table.genres),
        indices.map(table.languages),
        indices.map(table.createdAts)
      )

    val allComics: ComicTable = ??? // Load or insert data

    val recentFantasyComics = allComics
      .|>(filterByGenre(_, Genre("fantasy")))
      .|>(filterByCreatedAfter(_, Instant.now().minus(30, ChronoUnit.DAYS)))

    def countByLanguage(table: ComicTable): Map[Language, Int] =
      table.languages.groupMapReduce(identity)(_ => 1)(_ + _)

  }

}
