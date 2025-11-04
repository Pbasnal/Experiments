# Workflow App

A Scala-based workflow automation tool for processing Hadoop logs using YAML-defined workflows.

## Features

- **YAML-based Workflows**: Define complex data processing pipelines in simple YAML format
- **Hadoop Integration**: Leverages existing Python Hadoop tools from the pydoop project
- **Python Script Execution**: Execute custom Python scripts as workflow nodes
- **Dependency Management**: Automatic dependency resolution and execution ordering
- **Error Handling**: Robust error handling with retry capabilities
- **CLI Interface**: Simple command-line interface for workflow execution

## Quick Start

### 1. Build the Application

```bash
sbt assembly
```

This creates an executable JAR: `target/scala-2.12/workflow-app.jar`

### 2. Test Hadoop Connection

```bash
java -jar target/scala-2.12/workflow-app.jar test-hadoop
```

### 3. Create Sample Workflows

```bash
# Create Hadoop log fetching workflow
java -jar target/scala-2.12/workflow-app.jar create hadoop

# Create RPBC mismatch workflow  
java -jar target/scala-2.12/workflow-app.jar create rpbc
```

### 4. Run a Workflow

```bash
# Run with specific inputs
java -jar target/scala-2.12/workflow-app.jar run workflows/hadoop-sample.yaml --input datadate=20250129

# Run RPBC workflow
java -jar target/scala-2.12/workflow-app.jar run workflows/rpbc-sample.yaml --input datadate=20250129 --input hour=14
```

## Workflow Definition

### Basic Structure

```yaml
workflow:
  name: "my-workflow"
  version: "1.0"

nodes:
  - id: "fetch_data"
    type: "hadoop_fetch"
    module: "HadoopLogFetcher"
    config:
      query: |
        SELECT * FROM messaging.table 
        WHERE datadate = '${inputs.datadate}'
    outputs:
      - "query_results"

  - id: "process_data"
    type: "python_script"
    script: "scripts/process_data.py"
    depends_on: ["fetch_data"]
    inputs:
      raw_data: "${fetch_data.query_results}"
    outputs:
      - "processed_data"

execution:
  parallel: false
  max_retries: 3
  timeout: "30 minutes"
```

### Node Types

1. **hadoop_fetch**: Fetch data from Hadoop using SQL queries
   - Uses existing pydoop infrastructure
   - Supports variable substitution in queries
   - Automatic connection management

2. **python_script**: Execute custom Python scripts
   - Input/output via JSON files
   - Automatic dependency data passing
   - Error handling and logging

### Variable Substitution

- `${inputs.variable}`: User-provided inputs
- `${node_id.output}`: Output from previous nodes

## CLI Commands

### Execute Workflow
```bash
java -jar workflow-app.jar run <workflow.yaml> [--input key=value] ...
```

### Validate Workflow
```bash
java -jar workflow-app.jar validate <workflow.yaml>
```

### Create Sample Workflows
```bash
java -jar workflow-app.jar create [hadoop|rpbc]
```

### Test Hadoop Connection
```bash
java -jar workflow-app.jar test-hadoop
```

## Example Workflows

### 1. Hadoop Log Analysis

```yaml
workflow:
  name: "log-analysis"
  version: "1.0"

nodes:
  - id: "fetch_logs"
    type: "hadoop_fetch"
    module: "HadoopLogFetcher"
    config:
      query: |
        SELECT from_unixtime(cast(logtime / 1000 as int)) as log_time, *
        FROM messaging.AriCalculatorCalculationDroppedMessage
        WHERE datadate = '${inputs.datadate}'
        AND server LIKE 'SG-%'
        LIMIT 100
    outputs:
      - "query_results"

execution:
  timeout: "5 minutes"
```

### 2. RPBC Mismatch Detection

```yaml
workflow:
  name: "rpbc-mismatch"
  version: "1.0"

nodes:
  - id: "find_mismatches"
    type: "hadoop_fetch"
    module: "HadoopLogFetcher" 
    config:
      query: |
        SELECT datadate, hour, tuid, rpbc.`date`, rpbc.hotelid,
               roomtypeid, ratecategoryid,
               AVG(rpbc.minadvpurchase) AS avg_minadvpurchase,
               MAX(rpbc.minadvpurchase) AS max_minadvpurchase
        FROM messaging.propertyrateplanbookingconditionv2 AS rpbc
        WHERE rpbc.datadate = '${inputs.datadate}'
          AND dmcid = 332
          AND hour = '${inputs.hour}'
        GROUP BY datadate, tuid, rpbc.hotelid, roomtypeid, ratecategoryid, rpbc.`date`, hour
        HAVING AVG(rpbc.minadvpurchase) <> MAX(rpbc.minadvpurchase)
    outputs:
      - "query_results"
```

## Output Files

Execution results are stored in the `outputs/` directory:

```
outputs/
├── exec_<timestamp>/
│   ├── <node_id>_input.json     # Node inputs
│   ├── <node_id>_output.json    # Node outputs  
│   └── <node_id>_logs.txt       # Execution logs
```

## Error Handling

- **Validation**: Workflows are validated before execution
- **Retries**: Failed nodes can be retried (configurable)
- **Logging**: Comprehensive logging to files and console
- **Graceful Failures**: Clear error messages and exit codes

## Integration with Pydoop

The application integrates seamlessly with the existing pydoop Python tools:

- Uses existing `GenericHadoopQueryTool` for Hadoop connections
- Leverages `config.py` for connection credentials
- Maintains compatibility with existing Python scripts
- No duplication of connection logic

## Development

### Project Structure

```
src/main/scala/com/workflow/
├── WorkflowApp.scala           # Main application
├── core/
│   ├── WorkflowEngine.scala    # Execution engine
│   └── YamlParser.scala        # YAML parsing
├── executor/
│   └── PythonExecutor.scala    # Python script execution
├── modules/
│   └── HadoopLogFetcher.scala  # Hadoop integration
└── models/                     # Data models
```

### Building

```bash
# Compile
sbt compile

# Run tests
sbt test

# Create executable JAR
sbt assembly

# Run directly with SBT
sbt "run test-hadoop"
```

## Requirements

- Scala 2.12.17
- SBT 1.8.2
- Python 3.x with pydoop dependencies
- Access to Hadoop cluster (VPN if required)

## Troubleshooting

1. **Hadoop Connection Issues**: Run `test-hadoop` command first
2. **Python Module Errors**: Check that pydoop symlink exists in scripts/
3. **Permission Errors**: Ensure output directories are writable
4. **YAML Validation**: Use `validate` command to check workflow syntax