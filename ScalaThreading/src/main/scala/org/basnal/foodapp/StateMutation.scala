package org.basnal.foodapp

/** Pure state-threading abstraction; replicates concept used in previous experiments. */
case class StateMutation[S, A](run: S => (A, S)) {
  def map[B](f: A => B): StateMutation[S, B] =
    StateMutation { s =>
      val (a, next) = run(s); (f(a), next)
    }

  def flatMap[B](f: A => StateMutation[S, B]): StateMutation[S, B] =
    StateMutation { s =>
      val (a, next) = run(s); f(a).run(next)
    }
} 