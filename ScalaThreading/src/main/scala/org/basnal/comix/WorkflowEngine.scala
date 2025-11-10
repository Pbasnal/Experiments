package org.basnal.scala
package comix

import cats.effect.IO
import cats.implicits.catsSyntaxTuple3Parallel

object WorkflowEngine {
  trait Stage[F[_], A, B] {
    def run(input: A): F[B]
  }

  final case class Pipeline[F[_], A, B](run: A => F[B]) {
    def andThen[C](next: Pipeline[F, B, C])(implicit F: cats.Monad[F]): Pipeline[F, A, C] =
      Pipeline(a => F.flatMap(run(a))(next.run))

    def map[C](f: B => C)(implicit F: cats.Functor[F]): Pipeline[F, A, C] =
      Pipeline(a => F.map(run(a))(f))
  }

  final class PipelineBuilder[F[_] : cats.Monad, A, B] private(private val pipeline: Pipeline[F, A, B]) {
    def step[C](next: Pipeline[F, B, C]): PipelineBuilder[F, A, C] = {
      new PipelineBuilder(pipeline.andThen(next))
    }

    def build: Pipeline[F, A, B] = pipeline
  }

  object PipelineBuilder {
    def apply[F[_] : cats.Monad, A, B](stage: Pipeline[F, A, B]): PipelineBuilder[F, A, B] = new PipelineBuilder(stage)
  }

  // Sample domain processing
  case class UploadedPDF(path: String)

  case class Metadata(title: String, pageCount: Int)

  case class Thumbnails(sizes: Map[String, Array[Byte]])

  case class OCRText(text: String)

  case class ComicChapter(metadata: Metadata, thumbnails: Thumbnails, ocr: OCRText)


  val extractMetadata = Pipeline[IO, UploadedPDF, Metadata](pdf => IO(Metadata("My Comic", 12)))
  val generateThumbnails = Pipeline[IO, Metadata, Thumbnails](meta => IO(Thumbnails(Map("small" -> Array()))))
  val ocr = Pipeline[IO, Thumbnails, OCRText](thumbs => IO(OCRText("Translated text")))

  val builder = PipelineBuilder[IO, UploadedPDF, Metadata](extractMetadata)
    .step(generateThumbnails)
    .step(ocr)

  val finalPipeline: Pipeline[IO, UploadedPDF, OCRText] = builder.build

}
