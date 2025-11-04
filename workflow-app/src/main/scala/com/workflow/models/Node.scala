package com.workflow.models

case class Node(
  id: String,
  nodeType: String,  // "python_script", "hadoop_fetch", "data_process"
  script: Option[String] = None,
  module: Option[String] = None,  // For built-in modules like HadoopLogFetcher
  dependsOn: List[String] = List.empty,
  inputs: Map[String, String] = Map.empty,
  outputs: List[String] = List.empty,
  config: Map[String, String] = Map.empty
) {
  def isRootNode: Boolean = dependsOn.isEmpty
  
  def isScriptNode: Boolean = nodeType == "python_script" && script.isDefined
  
  def isModuleNode: Boolean = module.isDefined
  
  def getInputValue(key: String): Option[String] = inputs.get(key)
  
  def getConfigValue(key: String): Option[String] = config.get(key)
  
  def hasOutput(outputName: String): Boolean = outputs.contains(outputName)
}

object Node {
  def pythonScript(id: String, script: String, dependsOn: List[String] = List.empty): Node =
    Node(
      id = id,
      nodeType = "python_script",
      script = Some(script),
      dependsOn = dependsOn
    )
    
  def hadoopFetch(id: String, config: Map[String, String], dependsOn: List[String] = List.empty): Node =
    Node(
      id = id,
      nodeType = "hadoop_fetch",
      module = Some("HadoopLogFetcher"),
      config = config,
      dependsOn = dependsOn,
      outputs = List("query_results")
    )
}
