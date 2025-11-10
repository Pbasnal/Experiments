package org.basnal.scala.comix

import org.basnal.scala.comix.Domain.{ComicId, Language, UserId}

object Model {
  final case class User(
                         id: UserId,
                         email: String,
                         preferences: Preferences,
                         subscribedComics: List[ComicId]
                       )

  final case class Preferences(
                                preferredLanguages: List[Language],
                                notificationsEnabled: Boolean
                              )

}
