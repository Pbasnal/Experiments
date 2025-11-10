package org.basnal.scala.scalaquest

import org.basnal.scala.comix.Domain.ComicId

import java.time.Instant
import java.util.UUID

object MessageBusLibrary {
  sealed trait Event

  type Handler = PartialFunction[Event, () => Unit]


  case class ChapterUploaded(comicId: ComicId, timestamp: Instant) extends Event

  case class ComicMetadataUpdated(comicId: ComicId, fields: List[String]) extends Event

  case class NotifySubscribers(comicId: ComicId) extends Event


  class MessageBus private(private val handlers: Vector[Handler]) {

    def emit(event: Event): Unit = {
      handlers.foreach { handler =>
        if (handler.isDefinedAt(event)) {
          handler(event)() // Evaluate thunk
        }
      }
    }

    def subscribe(handler: Handler): MessageBus =
      new MessageBus(handlers :+ handler)
  }

  object MessageBus {
    def empty: MessageBus = new MessageBus(Vector.empty)
  }
}

object SampleUseOfMessageBus extends App {

  import org.basnal.scala.scalaquest.MessageBusLibrary._

  val bus = MessageBus.empty
    .subscribe {
      case ChapterUploaded(id, time) =>
        () => println(s"📦 Chapter uploaded for $id at $time")
    }
    .subscribe {
      case ComicMetadataUpdated(id, fields) =>
        () => println(s"📝 Comic $id updated: ${fields.mkString(", ")}")
    }
    .subscribe {
      case NotifySubscribers(id) =>
        () => println(s"📣 Notifying users about new chapter for comic $id")
    }

  // Trigger events
  bus.emit(ChapterUploaded(ComicId(UUID.randomUUID()), Instant.now()))
  bus.emit(ComicMetadataUpdated(ComicId(UUID.randomUUID()), List("title", "language")))

}

import org.basnal.scala.scalaquest.MessageBusLibrary._

class BatchMessageBus(handlers: Vector[Handler]) {
  private val queue = new java.util.concurrent.ConcurrentLinkedQueue[Event]()

  def emit(event: Event): Unit = queue.add(event)

  def dispatch(): Unit = {
    while (!queue.isEmpty) {
      val event = queue.poll()
      handlers.foreach(h => if (h.isDefinedAt(event)) h(event)())
    }
  }
}
