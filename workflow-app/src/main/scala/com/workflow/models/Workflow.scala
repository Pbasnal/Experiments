package com.workflow.models

import scala.concurrent.duration.Duration
import java.time.Instant

case class Workflow(
  name: String,
  version: String,
  nodes: List[Node],
  execution: ExecutionConfig
) {
  def getNode(nodeId: String): Option[Node] = nodes.find(_.id == nodeId)
  
  def getDependencies(nodeId: String): List[String] = 
    getNode(nodeId).map(_.dependsOn).getOrElse(List.empty)
}

case class ExecutionConfig(
  parallel: Boolean = false,
  maxRetries: Int = 3,
  timeout: Duration = Duration("30 minutes")
)

object Workflow {
  def empty: Workflow = Workflow(
    name = "",
    version = "1.0",
    nodes = List.empty,
    execution = ExecutionConfig()
  )
}
