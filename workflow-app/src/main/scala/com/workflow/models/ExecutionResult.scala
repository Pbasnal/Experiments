package com.workflow.models

import scala.concurrent.duration.Duration
import java.time.Instant

// Execution status enumeration
sealed trait ExecutionStatus
object ExecutionStatus {
  case object Pending extends ExecutionStatus
  case object Running extends ExecutionStatus
  case object Completed extends ExecutionStatus
  case object Failed extends ExecutionStatus
  case object Cancelled extends ExecutionStatus
  
  def fromString(s: String): ExecutionStatus = s.toLowerCase match {
    case "pending" => Pending
    case "running" => Running
    case "completed" => Completed
    case "failed" => Failed
    case "cancelled" => Cancelled
    case _ => Pending
  }
}

// Node execution status
sealed trait NodeStatus
object NodeStatus {
  case object Waiting extends NodeStatus
  case object Running extends NodeStatus
  case object Success extends NodeStatus
  case object Failed extends NodeStatus
  case object Skipped extends NodeStatus
  
  def fromString(s: String): NodeStatus = s.toLowerCase match {
    case "waiting" => Waiting
    case "running" => Running
    case "success" => Success
    case "failed" => Failed
    case "skipped" => Skipped
    case _ => Waiting
  }
}

case class ExecutionContext(
  executionId: String,
  workflowName: String,
  startTime: Instant,
  status: ExecutionStatus,
  nodeResults: Map[String, NodeResult] = Map.empty,
  endTime: Option[Instant] = None,
  error: Option[String] = None
) {
  def getNodeResult(nodeId: String): Option[NodeResult] = nodeResults.get(nodeId)
  
  def updateNodeResult(result: NodeResult): ExecutionContext = 
    copy(nodeResults = nodeResults + (result.nodeId -> result))
    
  def isCompleted: Boolean = status == ExecutionStatus.Completed
  
  def isFailed: Boolean = status == ExecutionStatus.Failed
  
  def duration: Option[Duration] = endTime.map { end =>
    Duration.fromNanos(java.time.Duration.between(startTime, end).toNanos)
  }
}

case class NodeResult(
  nodeId: String,
  status: NodeStatus,
  output: Map[String, Any] = Map.empty,
  logs: List[String] = List.empty,
  startTime: Option[Instant] = None,
  endTime: Option[Instant] = None,
  error: Option[String] = None
) {
  def executionTime: Option[Duration] = for {
    start <- startTime
    end <- endTime
  } yield Duration.fromNanos(java.time.Duration.between(start, end).toNanos)
  
  def isSuccess: Boolean = status == NodeStatus.Success
  
  def isFailed: Boolean = status == NodeStatus.Failed
  
  def getOutputValue(key: String): Option[Any] = output.get(key)
}

object ExecutionContext {
  def create(workflowName: String): ExecutionContext = {
    val executionId = s"exec_${System.currentTimeMillis()}"
    ExecutionContext(
      executionId = executionId,
      workflowName = workflowName,
      startTime = Instant.now(),
      status = ExecutionStatus.Pending
    )
  }
}

object NodeResult {
  def pending(nodeId: String): NodeResult = NodeResult(
    nodeId = nodeId,
    status = NodeStatus.Waiting
  )
  
  def running(nodeId: String): NodeResult = NodeResult(
    nodeId = nodeId,
    status = NodeStatus.Running,
    startTime = Some(Instant.now())
  )
  
  def success(nodeId: String, output: Map[String, Any], logs: List[String] = List.empty): NodeResult = NodeResult(
    nodeId = nodeId,
    status = NodeStatus.Success,
    output = output,
    logs = logs,
    endTime = Some(Instant.now())
  )
  
  def failed(nodeId: String, error: String, logs: List[String] = List.empty): NodeResult = NodeResult(
    nodeId = nodeId,
    status = NodeStatus.Failed,
    error = Some(error),
    logs = logs,
    endTime = Some(Instant.now())
  )
}
