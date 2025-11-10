package org.basnal.scala.comix

import java.time.Instant
import java.util.UUID

object Domain {
  // Core IDs
  final case class ComicId(value: UUID) extends AnyVal

  final case class ChapterId(value: UUID) extends AnyVal

  final case class ArtistId(value: UUID) extends AnyVal

  final case class UserId(value: UUID) extends AnyVal

  // Language/Genre tags
  final case class Language(code: String)

  final case class Genre(name: String)

  // Comic
  final case class Comic(
                          id: ComicId,
                          title: String,
                          artistId: ArtistId,
                          genres: List[Genre],
                          language: Language,
                          createdAt: Instant
                        )

  // Chapter
  final case class Chapter(
                            id: ChapterId,
                            comicId: ComicId,
                            number: Int,
                            title: String,
                            pages: Int,
                            uploadedAt: Instant,
                            scheduledAt: Option[Instant], // Optional scheduling
                            coverImage: Option[String], // URI or key to media
                            status: ChapterStatus
                          )

  sealed trait ChapterStatus

  object ChapterStatus {
    case object Draft extends ChapterStatus

    case object Processing extends ChapterStatus

    case object Scheduled extends ChapterStatus

    case object Published extends ChapterStatus

    case object Failed extends ChapterStatus
  }
}
