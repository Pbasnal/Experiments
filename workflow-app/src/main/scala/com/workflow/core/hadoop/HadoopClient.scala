package com.workflow.core.hadoop

import cats.effect.kernel.{Async, Resource}
import cats.syntax.all._
import com.workflow.core.algebra._
import java.sql.{Connection, DriverManager, ResultSet}
import scala.concurrent.duration._

/**
 * Hadoop client implementation using JDBC
 */
class HadoopClient[F[_]: Async] private (
  config: HadoopConfig,
  private[hadoop] val connection: Connection
) extends HadoopF[F] {

  def executeQuery(query: String, params: Map[String, String]): F[QueryResult] = {
    Async[F].delay {
      println(s"Executing query: $query")
      println(s"Parameters: $params")
      
      val stmt = connection.createStatement()
      stmt.setQueryTimeout(config.queryTimeout.toSeconds.toInt)
      
      val rs = stmt.executeQuery(query)
      val metadata = rs.getMetaData
      val columnCount = metadata.getColumnCount
      
      val columns = (1 to columnCount).map(i => metadata.getColumnName(i)).toList
      val rows = new scala.collection.mutable.ListBuffer[List[String]]()
      
      while (rs.next()) {
        val row = (1 to columnCount).map(i => Option(rs.getString(i)).getOrElse("")).toList
        rows += row
      }
      
      stmt.close()
      QueryResult(columns, rows.toList)
    }
  }

  def testConnection: F[ConnectionStatus] = {
    val testQuery = "SELECT 1 as test_value, current_timestamp() as current_time, current_database() as current_db"
    executeQuery(testQuery, Map.empty)
      .map(_ => ConnectionStatus.Connected: ConnectionStatus)
      .handleError { err =>
        println(s"Connection failed: ${err.getClass.getName} - ${err.getMessage}\n" +
          err.getStackTrace.take(5).map("  at " + _).mkString("\n"))
        ConnectionStatus.Failed(err.getMessage): ConnectionStatus
      }
  }
}

object HadoopClient {
  def resource[F[_]: Async](config: HadoopConfig): Resource[F, HadoopClient[F]] = {
    val acquire = Async[F].delay {
      // Load the Impala JDBC driver
      Class.forName("com.cloudera.impala.jdbc41.Driver")
      
      // Build the JDBC URL
      val url = s"jdbc:impala://${config.host}:${config.port}/${config.database};AuthMech=3;${config.jdbcParams}"
      println(s"Connecting to: $url")
      
      // Create the connection
      val conn = DriverManager.getConnection(url, config.username, config.password)
      
      // Create the client
      new HadoopClient[F](config, conn)
    }
    
    val release = (client: HadoopClient[F]) => Async[F].delay {
      client.connection.close()
    }
    
    Resource.make(acquire)(release)
  }
}

case class HadoopConfig(
  host: String,
  port: Int,
  database: String,
  username: String,
  password: String,
  queryTimeout: FiniteDuration = 5.minutes,
  clientTimeout: FiniteDuration = 1.minute,
  idleTimeout: FiniteDuration = 30.seconds
) {
  def jdbcParams: String = {
    val params = List(
      "SSL=0",
      "LogLevel=6",
      "LogPath=/tmp/impala_jdbc.log",
      s"UID=$username",
      s"PWD=$password"
    )
    params.mkString(";")
  }
}