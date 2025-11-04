package com.workflow.core.engine

import cats.effect.kernel.{Async, Clock, Temporal}
import cats.effect.std.{Random, Supervisor}
import cats.syntax.all._
import com.workflow.core.algebra._
import com.workflow.core.hadoop.HadoopClient
import fs2.Stream
import io.circe.yaml.parser
import org.typelevel.log4cats.Logger
import io.circe.syntax._

import scala.concurrent.duration._

class WorkflowEngine[F[_]: Async: Logger](
  hadoopClient: HadoopClient[F],
  supervisor: Supervisor[F]
) extends WorkflowF[F] {

  def readWorkflow(path: String): F[Workflow] = {
    import io.circe.yaml.parser
    import com.workflow.core.algebra.json._

    fs2.io.file.Files[F]
      .readAll(fs2.io.file.Path(path))
      .through(fs2.text.utf8.decode)
      .compile
      .string
      .flatMap { content =>
        Async[F].fromEither(
          parser.parse(content).flatMap(_.as[Workflow])
        )
      }
  }

  def validateWorkflow(workflow: Workflow): F[List[String]] = {
    val errors = scala.collection.mutable.ListBuffer[String]()

    // Check for cycles
    try {
      resolveExecutionOrder(workflow)
    } catch {
      case _: Exception =>
        errors += "Circular dependency detected in workflow"
    }

    // Check node references
    val nodeIds = workflow.nodes.map(_.id).toSet
    workflow.nodes.foreach { node =>
      node.dependsOn.foreach { depId =>
        if (!nodeIds.contains(depId)) {
          errors += s"Node ${node.id} depends on non-existent node $depId"
        }
      }
    }

    // Check node types
    workflow.nodes.foreach { node =>
      if (!Set("hadoop_fetch").contains(node.nodeType)) {
        errors += s"Node ${node.id} has unsupported type ${node.nodeType}"
      }
    }

    errors.toList.pure[F]
  }

  def executeWorkflow(workflow: Workflow, inputs: Map[String, String]): F[WorkflowResult] = {
    for {
      executionId <- Random.scalaUtilRandom[F].flatMap(_.nextString(8))
      startTime <- Clock[F].realTime
      context = WorkflowContext(workflow.name, executionId, inputs = inputs)
      order = resolveExecutionOrder(workflow)
      result <- executeNodes(workflow, context, order)
      endTime <- Clock[F].realTime
    } yield WorkflowResult(
      workflowId = workflow.name,
      status = if (result.nodeResults.values.exists(_.isFailed)) WorkflowStatus.Failed else WorkflowStatus.Success,
      nodeResults = result.nodeResults,
      startTime = startTime.toMillis,
      endTime = Some(endTime.toMillis)
    )
  }

  def executeNode(node: Node, context: WorkflowContext): F[NodeResult] = {
    for {
      startTime <- Clock[F].realTime
      result <- node.nodeType match {
        case "hadoop_fetch" =>
          executeHadoopNode(node, context)
        case _ =>
          NodeResult(
            nodeId = node.id,
            status = NodeStatus.Failed,
            error = Some(s"Unsupported node type: ${node.nodeType}")
          ).pure[F]
      }
      endTime <- Clock[F].realTime
    } yield result.copy(
      startTime = startTime.toMillis,
      endTime = Some(endTime.toMillis)
    )
  }

  private def executeNodes(
    workflow: Workflow,
    context: WorkflowContext,
    order: List[String]
  ): F[WorkflowContext] = {
    if (workflow.execution.parallel) {
      executeNodesParallel(workflow, context, order)
    } else {
      executeNodesSequential(workflow, context, order)
    }
  }

  private def executeNodesParallel(
    workflow: Workflow,
    context: WorkflowContext,
    order: List[String]
  ): F[WorkflowContext] = {
    val executions = order.traverse { nodeId =>
      workflow.nodes.find(_.id == nodeId).traverse { node =>
        supervisor.supervise(executeNode(node, context)).map(nodeId -> _)
      }
    }

    executions.flatMap { fibers =>
      fibers.flatten.traverse { case (nodeId, fiber) =>
        fiber.join.flatMap {
          case cats.effect.kernel.Outcome.Succeeded(fa) => fa.map(nodeId -> _)
          case cats.effect.kernel.Outcome.Errored(e) => 
            NodeResult(
              nodeId = nodeId,
              status = NodeStatus.Failed,
              error = Some(e.getMessage)
            ).pure[F].map(nodeId -> _)
          case cats.effect.kernel.Outcome.Canceled() =>
            NodeResult(
              nodeId = nodeId,
              status = NodeStatus.Failed,
              error = Some("Task was canceled")
            ).pure[F].map(nodeId -> _)
        }
      }.map { results =>
        results.foldLeft(context) { case (ctx, (nodeId, result)) =>
          ctx.withNodeResult(nodeId, result)
        }
      }
    }
  }

  private def executeNodesSequential(
    workflow: Workflow,
    context: WorkflowContext,
    order: List[String]
  ): F[WorkflowContext] = {
    order.foldM(context) { (ctx, nodeId) =>
      workflow.nodes.find(_.id == nodeId).traverse { node =>
        executeNode(node, ctx).map(result => ctx.withNodeResult(nodeId, result))
      }.map(_.getOrElse(ctx))
    }
  }

  private def executeHadoopNode(node: Node, context: WorkflowContext): F[NodeResult] = {
    val query = node.config.get("query").getOrElse("")
    val resolvedQuery = resolveVariables(query, context)

    hadoopClient.executeQuery(resolvedQuery, Map.empty)
      .map { result =>
        NodeResult(
          nodeId = node.id,
          status = NodeStatus.Success,
          outputs = Map(
            "query_results" -> result.asJson(json.queryResultEncoder).noSpaces,
            "row_count" -> result.rows.size.toString
          )
        )
      }
      .handleError { error =>
        NodeResult(
          nodeId = node.id,
          status = NodeStatus.Failed,
          error = Some(error.getMessage)
        )
      }
  }

  private def resolveExecutionOrder(workflow: Workflow): List[String] = {
    val graph = workflow.nodes.map(node => node.id -> node.dependsOn).toMap
    val inDegree = scala.collection.mutable.Map[String, Int]()
    
    // Initialize in-degrees
    workflow.nodes.foreach { node =>
      inDegree(node.id) = node.dependsOn.length
    }
    
    // Find nodes with no dependencies
    val queue = scala.collection.mutable.Queue[String]()
    inDegree.filter(_._2 == 0).keys.foreach(queue.enqueue(_))
    
    val result = scala.collection.mutable.ListBuffer[String]()
    
    while (queue.nonEmpty) {
      val current = queue.dequeue()
      result += current
      
      // Find nodes that depend on current node
      workflow.nodes.filter(_.dependsOn.contains(current)).foreach { node =>
        inDegree(node.id) -= 1
        if (inDegree(node.id) == 0) {
          queue.enqueue(node.id)
        }
      }
    }
    
    if (result.length != workflow.nodes.length) {
      throw new RuntimeException("Circular dependency detected in workflow")
    }
    
    result.toList
  }

  private def resolveVariables(text: String, context: WorkflowContext): String = {
    val pattern = """\$\{([^}]+)\}""".r
    pattern.replaceAllIn(text, m => {
      val path = m.group(1)
      if (path.startsWith("inputs.")) {
        val key = path.substring("inputs.".length)
        context.inputs.getOrElse(key, m.group(0))
      } else {
        val parts = path.split("\\.")
        parts match {
          case Array(nodeId, outputKey) =>
            context.nodeResults.get(nodeId)
              .flatMap(_.outputs.get(outputKey))
              .getOrElse(m.group(0))
          case _ => m.group(0)
        }
      }
    })
  }
}