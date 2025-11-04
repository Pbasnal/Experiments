package com.workflow.core.engine

import cats.effect.IO
import cats.effect.std.Supervisor
import cats.effect.testing.scalatest.AsyncIOSpec
import com.workflow.core.algebra._
import com.workflow.core.hadoop.{HadoopClient, HadoopConfig}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger

class WorkflowEngineSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  def withLogger[A](f: org.typelevel.log4cats.Logger[IO] => IO[A]): IO[A] =
    Slf4jLogger.create[IO].flatMap { implicit logger =>
      f(logger)
    }

  "WorkflowEngine" - {
    "should execute a simple workflow" in {
      val testWorkflow = Workflow(
        name = "test-workflow",
        version = "1.0",
        nodes = List(
          Node(
            id = "test-node",
            nodeType = "hadoop_fetch",
            config = Map(
              "query" -> "SELECT 1"
            )
          )
        ),
        execution = ExecutionConfig(
          parallel = false,
          maxRetries = 1,
          timeout = "1 minute"
        )
      )

      val mockHadoopClient = new HadoopClient[IO](
        HadoopConfig("", 0, "", "", ""),
        null
      ) {
        override def executeQuery(query: String, params: Map[String, String]): IO[QueryResult] =
          IO.pure(QueryResult(
            columns = List("test"),
            rows = List(List("1"))
          ))

        override def testConnection: IO[ConnectionStatus] =
          IO.pure(ConnectionStatus.Connected: ConnectionStatus)
      }

      val program = withLogger { implicit logger =>
        Supervisor[IO].use { supervisor =>
          val engine = new WorkflowEngine[IO](mockHadoopClient, supervisor)
          engine.executeWorkflow(testWorkflow, Map.empty)
        }
      }

      program.asserting { (result: WorkflowResult) =>
        result.status shouldBe WorkflowStatus.Success
        result.nodeResults.size shouldBe 1
        result.nodeResults("test-node").status shouldBe NodeStatus.Success
      }
    }

    "should handle parallel execution" in {
      val testWorkflow = Workflow(
        name = "test-workflow",
        version = "1.0",
        nodes = List(
          Node(
            id = "node1",
            nodeType = "hadoop_fetch",
            config = Map(
              "query" -> "SELECT 1"
            )
          ),
          Node(
            id = "node2",
            nodeType = "hadoop_fetch",
            config = Map(
              "query" -> "SELECT 2"
            )
          )
        ),
        execution = ExecutionConfig(
          parallel = true,
          maxRetries = 1,
          timeout = "1 minute"
        )
      )

      val mockHadoopClient = new HadoopClient[IO](
        HadoopConfig("", 0, "", "", ""),
        null
      ) {
        override def executeQuery(query: String, params: Map[String, String]): IO[QueryResult] =
          IO.pure(QueryResult(
            columns = List("test"),
            rows = List(List(query.last.toString))
          ))

        override def testConnection: IO[ConnectionStatus] =
          IO.pure(ConnectionStatus.Connected: ConnectionStatus)
      }

      val program = withLogger { implicit logger =>
        Supervisor[IO].use { supervisor =>
          val engine = new WorkflowEngine[IO](mockHadoopClient, supervisor)
          engine.executeWorkflow(testWorkflow, Map.empty)
        }
      }

      program.asserting { (result: WorkflowResult) =>
        result.status shouldBe WorkflowStatus.Success
        result.nodeResults.size shouldBe 2
        result.nodeResults("node1").status shouldBe NodeStatus.Success
        result.nodeResults("node2").status shouldBe NodeStatus.Success
      }
    }

    "should handle failures" in {
      val testWorkflow = Workflow(
        name = "test-workflow",
        version = "1.0",
        nodes = List(
          Node(
            id = "failing-node",
            nodeType = "hadoop_fetch",
            config = Map(
              "query" -> "SELECT 1"
            )
          )
        ),
        execution = ExecutionConfig(
          parallel = false,
          maxRetries = 1,
          timeout = "1 minute"
        )
      )

      val mockHadoopClient = new HadoopClient[IO](
        HadoopConfig("", 0, "", "", ""),
        null
      ) {
        override def executeQuery(query: String, params: Map[String, String]): IO[QueryResult] =
          IO.raiseError(new RuntimeException("Test error"))

        override def testConnection: IO[ConnectionStatus] =
          IO.pure(ConnectionStatus.Failed("Test error"): ConnectionStatus)
      }

      val program = withLogger { implicit logger =>
        Supervisor[IO].use { supervisor =>
          val engine = new WorkflowEngine[IO](mockHadoopClient, supervisor)
          engine.executeWorkflow(testWorkflow, Map.empty)
        }
      }

      program.asserting { (result: WorkflowResult) =>
        result.status shouldBe WorkflowStatus.Failed
        result.nodeResults.size shouldBe 1
        result.nodeResults("failing-node").status shouldBe NodeStatus.Failed
        result.nodeResults("failing-node").error shouldBe defined
      }
    }
  }
}