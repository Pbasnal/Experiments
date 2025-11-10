package org.basnal.scala.comix

import org.basnal.scala.comix.Domain.{ArtistId, ChapterId, ComicId, UserId}

import java.time.Instant

sealed trait Command

object Command {
  final case class UploadChapter(
                                  comicId: ComicId,
                                  artistId: ArtistId,
                                  chapterTitle: String,
                                  number: Int,
                                  pdfData: Array[Byte]
                                ) extends Command

  final case class ScheduleChapter(
                                    chapterId: ChapterId,
                                    publishAt: Instant
                                  ) extends Command

  final case class SubscribeUser(
                                  userId: UserId,
                                  comicId: ComicId
                                ) extends Command
}
