package org.basnal.foodapp

import cats.{Functor, Monad}

/** Composable effectful stages – lets us build side-effecting workflows declaratively. */
final case class Pipeline[F[_], A, B](run: A => F[B]) {
  def andThen[C](next: Pipeline[F, B, C])(implicit F: Monad[F]): Pipeline[F, A, C] =
    Pipeline(a => F.flatMap(run(a))(next.run))
  def map[C](f: B => C)(implicit F: Functor[F]): Pipeline[F, A, C] =
    Pipeline(a => F.map(run(a))(f))
}

final class PipelineBuilder[F[_]: Monad, A, B] private(private val p: Pipeline[F, A, B]) {
  def step[C](next: Pipeline[F, B, C]): PipelineBuilder[F, A, C] =
    new PipelineBuilder(p.andThen(next))
  def build: Pipeline[F, A, B] = p
}
object PipelineBuilder {
  def apply[F[_]: Monad, A, B](first: Pipeline[F, A, B]): PipelineBuilder[F, A, B] =
    new PipelineBuilder(first)
} 