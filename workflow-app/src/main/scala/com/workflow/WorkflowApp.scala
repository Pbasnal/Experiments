package com.workflow

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.effect.std.{Console, Supervisor}
import cats.syntax.all._
import com.workflow.core.algebra._
import com.workflow.core.engine.WorkflowEngine
import com.workflow.core.hadoop.{HadoopClient, HadoopConfig}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration._

object WorkflowApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    for {
      logger <- Slf4jLogger.create[IO]
      config <- IO.pure(HadoopConfig(
        host = "impala.sgprod.hadoop.agoda.local",
        port = 21050,
        database = "smapi",
        username = "hk-suppio-svc@hkg.agoda.local",
        password = "C{r1vKTJE9ZBOVb8Ck6t")
      )
      _ <- Supervisor[IO].use { supervisor =>
        HadoopClient.resource[IO](config).use { hadoopClient =>
          implicit val l = logger
          val engine = new WorkflowEngine[IO](hadoopClient, supervisor)
          val console = ConsoleF.create[IO]
          val app = new WorkflowAppLogic[IO](engine, console, logger)
          
          if (args.isEmpty) {
            app.showInteractiveMenu
          } else {
            app.handleCommand(args)
          }
        }
      }
    } yield ExitCode.Success
  }
}

class WorkflowAppLogic[F[_]](
  engine: WorkflowF[F],
  console: ConsoleF[F],
  logger: org.typelevel.log4cats.Logger[F]
)(implicit F: cats.effect.kernel.Async[F]) {

  def showInteractiveMenu: F[Unit] = {
    def printMenu: F[Unit] = {
      console.putStrLn("\nWorkflow App - Interactive Menu") *>
      console.putStrLn("=" * 40) *>
      console.putStrLn("1. Run a workflow") *>
      console.putStrLn("2. Validate a workflow") *>
      console.putStrLn("3. Create sample workflow") *>
      console.putStrLn("4. Test Hadoop connection") *>
      console.putStrLn("5. Show help") *>
      console.putStrLn("0. Exit") *>
      console.putStrLn("=" * 40) *>
      console.putStrLn("Enter your choice (0-5): ")
    }

    def promptWorkflowFile: F[Option[String]] = {
      for {
        _ <- console.putStrLn("\nAvailable workflows:")
        files <- fs2.io.file.Files[F].list(fs2.io.file.Path("workflows"))
          .filter(_.toString.endsWith(".yaml"))
          .compile
          .toList
        _ <- files.zipWithIndex.traverse { case (file, idx) =>
          console.putStrLn(s"${idx + 1}. ${file.fileName}")
        }
        _ <- console.putStrLn("\nEnter workflow file path (or press Enter to cancel): ")
        input <- console.readLine
      } yield Option(input).filter(_.nonEmpty)
    }

    def promptInputs: F[Map[String, String]] = {
      def loop(acc: Map[String, String]): F[Map[String, String]] = {
        for {
          _ <- console.putStrLn("Enter input key (or press Enter to finish): ")
          key <- console.readLine
          result <- if (key.isEmpty) {
            F.pure(acc)
          } else {
            for {
              _ <- console.putStrLn(s"Enter value for $key: ")
              value <- console.readLine
              result <- loop(acc + (key -> value))
            } yield result
          }
        } yield result
      }

      loop(Map.empty)
    }

    def handleChoice(choice: String): F[Boolean] = choice match {
      case "1" => // Run workflow
        promptWorkflowFile.flatMap {
          case Some(file) =>
            for {
              _ <- console.putStrLn("\nEnter workflow inputs:")
              inputs <- promptInputs
              _ <- runWorkflow(file, inputs)
            } yield true
          case None =>
            F.pure(true)
        }

      case "2" => // Validate workflow
        promptWorkflowFile.flatMap {
          case Some(file) => validateWorkflow(file).map(_ => true)
          case None => F.pure(true)
        }

      case "3" => // Create sample
        for {
          _ <- console.putStrLn("\nAvailable sample types:")
          _ <- console.putStrLn("1. Hadoop log fetching workflow")
          _ <- console.putStrLn("2. RPBC mismatch workflow")
          _ <- console.putStrLn("\nEnter your choice (1-2): ")
          choice <- console.readLine
          _ <- choice match {
            case "1" => createSampleWorkflow("hadoop")
            case "2" => createSampleWorkflow("rpbc")
            case _ => console.putStrLn("Invalid choice")
          }
        } yield true

      case "4" => // Test Hadoop
        testHadoopConnection.map(_ => true)

      case "5" => // Help
        showHelp.map(_ => true)

      case "0" => // Exit
        console.putStrLn("Goodbye!").map(_ => false)

      case _ =>
        console.putStrLn("Invalid choice, please try again").map(_ => true)
    }

    def loop: F[Unit] = {
      for {
        _ <- printMenu
        choice <- console.readLine
        continue <- handleChoice(choice)
        _ <- if (continue) {
          console.putStrLn("\nPress Enter to continue...") *>
          console.readLine *>
          loop
        } else {
          F.unit
        }
      } yield ()
    }

    loop
  }

  def handleCommand(args: List[String]): F[Unit] = {
    args.headOption match {
      case Some("run") => runWorkflow(args(1), parseInputs(args.drop(2)))
      case Some("validate") => validateWorkflow(args(1))
      case Some("create") => createSampleWorkflow(args(1))
      case Some("test-hadoop") => testHadoopConnection
      case Some("help") => showHelp
      case Some(cmd) =>
        console.putStrLn(s"Unknown command: $cmd") *>
        showHelp
      case None =>
        showHelp
    }
  }

  private def runWorkflow(file: String, inputs: Map[String, String]): F[Unit] = {
    for {
      workflow <- engine.readWorkflow(file)
      _ <- console.putStrLn(s"Running workflow: ${workflow.name} v${workflow.version}")
      result <- engine.executeWorkflow(workflow, inputs)
      _ <- printWorkflowResult(result)
    } yield ()
  }

  private def validateWorkflow(file: String): F[Unit] = {
    for {
      workflow <- engine.readWorkflow(file)
      errors <- engine.validateWorkflow(workflow)
      _ <- if (errors.isEmpty) {
        console.putStrLn("✅ Workflow is valid") *>
        console.putStrLn(s"   Name: ${workflow.name}") *>
        console.putStrLn(s"   Version: ${workflow.version}") *>
        console.putStrLn(s"   Nodes: ${workflow.nodes.length}")
      } else {
        console.putStrLn("❌ Workflow validation failed:") *>
        errors.traverse_(err => console.putStrLn(s"   - $err"))
      }
    } yield ()
  }

  private def createSampleWorkflow(workflowType: String): F[Unit] = {
    val content = workflowType match {
      case "hadoop" => hadoopSampleWorkflow
      case "rpbc" => rpbcSampleWorkflow
      case _ =>
        return console.putStrLn("Usage: create [hadoop|rpbc]") *>
               console.putStrLn("Available sample workflows:") *>
               console.putStrLn("  hadoop - Basic Hadoop log fetching workflow") *>
               console.putStrLn("  rpbc   - RPBC mismatch checking workflow")
    }

    val filename = s"workflows/${workflowType}-sample.yaml"
    for {
      _ <- fs2.io.file.Files[F].createDirectories(fs2.io.file.Path("workflows"))
      _ <- fs2.Stream
            .emit(content)
            .through(fs2.text.utf8.encode)
            .through(fs2.io.file.Files[F].writeAll(fs2.io.file.Path(filename)))
            .compile
            .drain
      _ <- console.putStrLn(s"Created: $filename")
      _ <- console.putStrLn(s"Run with: workflow-app run $filename --input datadate=20250129")
    } yield ()
  }

  private def testHadoopConnection: F[Unit] = {
    for {
      _ <- console.putStrLn("Testing Hadoop connection...")
      status <- engine.executeWorkflow(
        testWorkflow,
        Map("datadate" -> "20250129")
      )
      _ <- if (status.isSuccess) {
        console.putStrLn("✅ Hadoop connection successful")
      } else {
        console.putStrLn("❌ Hadoop connection failed") *>
        status.error.traverse_(err => console.putStrLn(s"   Error: $err"))
      }
    } yield ()
  }

  private def showHelp: F[Unit] = {
    console.putStrLn("""
      |Workflow App - Hadoop Log Processing Tool
      |
      |Usage: workflow-app [<command> [options]]
      |
      |Interactive Mode:
      |  Run without arguments to start interactive menu
      |
      |Commands:
      |  run <workflow.yaml> [--input key=value] ...  Execute a workflow
      |  validate <workflow.yaml>                     Validate workflow syntax
      |  create [hadoop|rpbc]                         Create sample workflows
      |  test-hadoop                                  Test Hadoop connection
      |  help                                         Show this help
      |
      |Examples:
      |  workflow-app                                 Start interactive menu
      |  workflow-app run workflows/hadoop-sample.yaml --input datadate=20250129
      |  workflow-app validate workflows/my-workflow.yaml
      |  workflow-app create rpbc
      |  workflow-app test-hadoop
      |""".stripMargin)
  }

  private def printWorkflowResult(result: WorkflowResult): F[Unit] = {
    val duration = result.duration.map(d => s"${d}ms").getOrElse("unknown")
    
    for {
      _ <- console.putStrLn("\n" + "=" * 60)
      _ <- console.putStrLn("EXECUTION SUMMARY")
      _ <- console.putStrLn("=" * 60)
      _ <- console.putStrLn(s"Workflow: ${result.workflowId}")
      _ <- console.putStrLn(s"Status: ${result.status}")
      _ <- console.putStrLn(s"Duration: $duration")
      _ <- result.error.traverse_(err => console.putStrLn(s"Error: $err"))
      _ <- console.putStrLn("\nNode Results:")
      _ <- result.nodeResults.toList.traverse { case (nodeId, node) =>
        val status = node.status match {
        case NodeStatus.Success => "✅"
        case NodeStatus.Failed => "❌"
        case NodeStatus.Running => "⏳"
          case NodeStatus.Pending => "⏸️"
        case NodeStatus.Skipped => "⏭️"
      }
        for {
          _ <- console.putStrLn(s"  $status $nodeId (${node.status})")
          _ <- node.error.traverse_(err => console.putStrLn(s"      Error: $err"))
          _ <- if (node.outputs.nonEmpty) {
            console.putStrLn(s"      Outputs: ${node.outputs.keys.mkString(", ")}")
          } else F.unit
          _ <- node.duration.traverse_(d => console.putStrLn(s"      Duration: ${d}ms"))
        } yield ()
      }
      _ <- console.putStrLn("=" * 60)
    } yield ()
  }

  private def parseInputs(args: List[String]): Map[String, String] = {
    args.sliding(2, 2).collect {
      case "--input" :: value :: Nil =>
        value.split("=", 2) match {
          case Array(key, value) => Some(key -> value)
          case _ => None
        }
    }.flatten.toMap
  }

  private val testWorkflow = Workflow(
    name = "test-connection",
    version = "1.0",
    nodes = List(
      Node(
        id = "test",
        nodeType = "hadoop_fetch",
        config = Map(
          "query" -> """
            |SELECT 1 as test_value, 
            |       current_timestamp() as current_time,
            |       current_database() as current_db
            |""".stripMargin
        )
      )
    ),
    execution = ExecutionConfig(
      parallel = false,
      maxRetries = 1,
      timeout = "1 minute"
    )
  )

  private def hadoopSampleWorkflow: String =
    """workflow:
      |  name: "hadoop-log-fetch"
      |  version: "1.0"
      |
      |nodes:
      |  - id: "fetch_logs"
      |    type: "hadoop_fetch"
      |    module: "HadoopLogFetcher"
      |    config:
      |      query: |
      |        SELECT from_unixtime(cast(logtime / 1000 as int)) as log_time, *
      |        FROM messaging.AriCalculatorCalculationDroppedMessage
      |        WHERE datadate = '${inputs.datadate}'
      |        AND server LIKE 'SG-%'
      |        LIMIT 10
      |    outputs:
      |      - "query_results"
      |
      |execution:
      |  parallel: false
      |  max_retries: 3
      |  timeout: "5 minutes"
      |""".stripMargin
    
  private def rpbcSampleWorkflow: String =
    """workflow:
      |  name: "rpbc-mismatch"
      |  version: "1.0"
      |  description: "Sample workflow for RPBC mismatch analysis"
      |
      |nodes:
      |  - id: "fetch_mismatches"
      |    type: "hadoop_fetch"
      |    module: "HadoopLogFetcher"
      |    config:
      |      query: |
      |        SELECT datadate, hour, tuid, rpbc.`date`, rpbc.hotelid,
      |               roomtypeid, ratecategoryid,
      |               AVG(rpbc.minadvpurchase) AS avg_minadvpurchase,
      |               MAX(rpbc.minadvpurchase) AS max_minadvpurchase
      |        FROM messaging.propertyrateplanbookingconditionv2 AS rpbc
      |        WHERE rpbc.datadate = '${inputs.datadate}'
      |          AND dmcid = 332
      |          AND hour = '${inputs.hour}'
      |        GROUP BY datadate, tuid, rpbc.hotelid, roomtypeid, ratecategoryid, rpbc.`date`, hour
      |        HAVING AVG(rpbc.minadvpurchase) <> MAX(rpbc.minadvpurchase)
      |    outputs:
      |      - "query_results"
      |
      |execution:
      |  parallel: false
      |  max_retries: 3
      |  timeout: "10 minutes"
      |""".stripMargin
}