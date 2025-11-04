ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "com.workflow"

val catsVersion = "2.9.0"
val catsEffectVersion = "3.5.1"
val fs2Version = "3.7.0"
val http4sVersion = "0.23.23"
val circeVersion = "0.14.5"
val log4catsVersion = "2.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "workflow-app",
    libraryDependencies ++= Seq(
      // Core libraries
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,

      // Impala JDBC driver
      "com.cloudera.impala" % "ImpalaJDBC41" % "2.6.30.1050",

      // JSON handling
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-yaml" % "0.15.1",

      // Logging
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.12",

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
    ),
    assembly / mainClass := Some("com.workflow.WorkflowApp"),
    assembly / assemblyJarName := "workflow-app.jar",
    
    // Add assembly plugin
    addCommandAlias("build", "assembly"),
    
    // Assembly merge strategy
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class") => MergeStrategy.discard
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )