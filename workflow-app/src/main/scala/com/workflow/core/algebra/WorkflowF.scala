package com.workflow.core.algebra

import cats.effect.kernel.{Concurrent, Resource}
import cats.syntax.all._
import cats.effect.std.Console
import fs2.Stream
import io.circe.{Decoder, Encoder}

/**
 * Base algebra for workflow operations
 */
trait WorkflowF[F[_]] {
  def readWorkflow(path: String): F[Workflow]
  def validateWorkflow(workflow: Workflow): F[List[String]]
  def executeWorkflow(workflow: Workflow, inputs: Map[String, String]): F[WorkflowResult]
  def executeNode(node: Node, context: WorkflowContext): F[NodeResult]
}

/**
 * Base algebra for Hadoop operations
 */
trait HadoopF[F[_]] {
  def executeQuery(query: String, params: Map[String, String]): F[QueryResult]
  def testConnection: F[ConnectionStatus]
}

/**
 * Base algebra for console operations
 */
trait ConsoleF[F[_]] {
  def readLine: F[String]
  def readLineWithPrompt(prompt: String): F[String]
  def putStrLn(line: String): F[Unit]
  def putError(error: String): F[Unit]
}

object ConsoleF {
  def apply[F[_]](implicit F: ConsoleF[F]): ConsoleF[F] = F

  def create[F[_]: Concurrent: Console]: ConsoleF[F] = new ConsoleF[F] {
    def readLine: F[String] = Console[F].readLine
    def readLineWithPrompt(prompt: String): F[String] = 
      Console[F].print(prompt) *> Console[F].readLine
    def putStrLn(line: String): F[Unit] = Console[F].println(line)
    def putError(error: String): F[Unit] = Console[F].errorln(error)
  }
}

/**
 * Core domain models
 */
case class Workflow(
  name: String,
  version: String,
  nodes: List[Node],
  execution: ExecutionConfig
)

case class Node(
  id: String,
  nodeType: String,
  config: Map[String, String],
  dependsOn: List[String] = List.empty,
  inputs: Map[String, String] = Map.empty,
  outputs: List[String] = List.empty
)

case class ExecutionConfig(
  parallel: Boolean,
  maxRetries: Int,
  timeout: String
)

case class WorkflowContext(
  workflowId: String,
  executionId: String,
  nodeResults: Map[String, NodeResult] = Map.empty,
  inputs: Map[String, String] = Map.empty
) {
  def withNodeResult(nodeId: String, result: NodeResult): WorkflowContext =
    copy(nodeResults = nodeResults + (nodeId -> result))
}

case class NodeResult(
  nodeId: String,
  status: NodeStatus,
  outputs: Map[String, String] = Map.empty,
  error: Option[String] = None,
  startTime: Long = System.currentTimeMillis(),
  endTime: Option[Long] = None
) {
  def duration: Option[Long] = endTime.map(_ - startTime)
  def isSuccess: Boolean = status == NodeStatus.Success
  def isFailed: Boolean = status == NodeStatus.Failed
}

sealed trait NodeStatus
object NodeStatus {
  case object Pending extends NodeStatus
  case object Running extends NodeStatus
  case object Success extends NodeStatus
  case object Failed extends NodeStatus
  case object Skipped extends NodeStatus
}

case class WorkflowResult(
  workflowId: String,
  status: WorkflowStatus,
  nodeResults: Map[String, NodeResult],
  error: Option[String] = None,
  startTime: Long = System.currentTimeMillis(),
  endTime: Option[Long] = None
) {
  def duration: Option[Long] = endTime.map(_ - startTime)
  def isSuccess: Boolean = status == WorkflowStatus.Success
  def isFailed: Boolean = status == WorkflowStatus.Failed
}

sealed trait WorkflowStatus
object WorkflowStatus {
  case object Running extends WorkflowStatus
  case object Success extends WorkflowStatus
  case object Failed extends WorkflowStatus
}

case class QueryResult(
  columns: List[String],
  rows: List[List[String]],
  metadata: Map[String, String] = Map.empty
)

sealed trait ConnectionStatus
object ConnectionStatus {
  sealed trait Connected extends ConnectionStatus
  case object Connected extends Connected
  case class Failed(error: String) extends ConnectionStatus
}

/**
 * JSON codecs
 */
object json {
  import io.circe.{Encoder, Decoder}
  import io.circe.generic.semiauto._

  implicit val nodeStatusEncoder: Encoder[NodeStatus] = Encoder.encodeString.contramap {
    case NodeStatus.Pending => "pending"
    case NodeStatus.Running => "running"
    case NodeStatus.Success => "success"
    case NodeStatus.Failed => "failed"
    case NodeStatus.Skipped => "skipped"
  }

  implicit val nodeStatusDecoder: Decoder[NodeStatus] = Decoder.decodeString.emap {
    case "pending" => Right(NodeStatus.Pending)
    case "running" => Right(NodeStatus.Running)
    case "success" => Right(NodeStatus.Success)
    case "failed" => Right(NodeStatus.Failed)
    case "skipped" => Right(NodeStatus.Skipped)
    case other => Left(s"Unknown NodeStatus: $other")
  }

  implicit val workflowStatusEncoder: Encoder[WorkflowStatus] = Encoder.encodeString.contramap {
    case WorkflowStatus.Running => "running"
    case WorkflowStatus.Success => "success"
    case WorkflowStatus.Failed => "failed"
  }

  implicit val workflowStatusDecoder: Decoder[WorkflowStatus] = Decoder.decodeString.emap {
    case "running" => Right(WorkflowStatus.Running)
    case "success" => Right(WorkflowStatus.Success)
    case "failed" => Right(WorkflowStatus.Failed)
    case other => Left(s"Unknown WorkflowStatus: $other")
  }

  implicit val nodeEncoder: Encoder[Node] = deriveEncoder
  implicit val nodeDecoder: Decoder[Node] = new Decoder[Node] {
    def apply(c: io.circe.HCursor): Decoder.Result[Node] = for {
      id <- c.get[String]("id")
      nodeType <- c.get[String]("type")
      config <- c.getOrElse[Map[String, String]]("config")(Map.empty)
      dependsOn <- c.getOrElse[List[String]]("dependsOn")(List.empty)
      inputs <- c.getOrElse[Map[String, String]]("inputs")(Map.empty)
      outputs <- c.getOrElse[List[String]]("outputs")(List.empty)
    } yield Node(id, nodeType, config, dependsOn, inputs, outputs)
  }

  implicit val executionConfigEncoder: Encoder[ExecutionConfig] = deriveEncoder
  implicit val executionConfigDecoder: Decoder[ExecutionConfig] = new Decoder[ExecutionConfig] {
    def apply(c: io.circe.HCursor): Decoder.Result[ExecutionConfig] = for {
      parallel <- c.getOrElse[Boolean]("parallel")(false)
      maxRetries <- c.downField("max_retries").as[Int]
      timeout <- c.get[String]("timeout")
    } yield ExecutionConfig(parallel, maxRetries, timeout)
  }

  implicit val workflowEncoder: Encoder[Workflow] = deriveEncoder
  implicit val workflowDecoder: Decoder[Workflow] = new Decoder[Workflow] {
    def apply(c: io.circe.HCursor): Decoder.Result[Workflow] = {
      val workflowCursor = c.downField("workflow")
      for {
        name <- workflowCursor.get[String]("name")
        version <- workflowCursor.get[String]("version")
        nodes <- c.get[List[Node]]("nodes")
        execution <- c.get[ExecutionConfig]("execution")
      } yield Workflow(name, version, nodes, execution)
    }
  }

  implicit val nodeResultEncoder: Encoder[NodeResult] = deriveEncoder
  implicit val nodeResultDecoder: Decoder[NodeResult] = deriveDecoder

  implicit val workflowResultEncoder: Encoder[WorkflowResult] = deriveEncoder
  implicit val workflowResultDecoder: Decoder[WorkflowResult] = deriveDecoder

  implicit val queryResultEncoder: Encoder[QueryResult] = deriveEncoder
  implicit val queryResultDecoder: Decoder[QueryResult] = deriveDecoder
}
