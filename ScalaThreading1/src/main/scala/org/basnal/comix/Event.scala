package org.basnal.scala.comix

import org.basnal.scala.comix.Domain.{ChapterId, UserId}

import java.time.Instant

sealed trait Event

object Event {
  final case class ChapterUploaded(chapterId: ChapterId, time: Instant) extends Event
  final case class ChapterScheduled(chapterId: ChapterId, publishAt: Instant) extends Event
  final case class ChapterPublished(chapterId: ChapterId, time: Instant) extends Event
  final case class NotificationSent(userId: UserId, chapterId: ChapterId) extends Event
}
