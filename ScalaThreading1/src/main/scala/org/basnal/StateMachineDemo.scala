package org.basnal.scala

sealed trait State {
  def nextState(input: String): State
}

case object Init extends State {
  override def nextState(input: String): State = input match {
    case "start" => Active
    case _ => this // Stay in the same state for unknown input
  }
}

case object Active extends State {
  override def nextState(input: String): State = input match {
    case "pause" => Paused
    case "stop" => Ended
    case _ => this
  }
}

case object Paused extends State {
  override def nextState(input: String): State = input match {
    case "resume" => Active
    case "stop" => Ended
    case _ => this
  }
}

case object Ended extends State {
  override def nextState(input: String): State = this // No transitions from "Ended"
}

class StateMachine(var currentState: State) {
  def processInput(input: String): Unit = {
    println(s"Current state: $currentState")
    currentState = currentState.nextState(input)
    println(s"Transitioned to: $currentState")
  }
}

object StateMachineDemo extends App {
  val stateMachine = new StateMachine(Init)

  stateMachine.processInput("start")
  stateMachine.processInput("pause")
  stateMachine.processInput("resume")
  stateMachine.processInput("stop")
}

object States extends Enumeration {
  type StateEnum = Value  // Define a type alias for convenience

  // Define the states
  val Init, Active, Paused, Ended = Value

  // Define the transition logic
  def nextState(currentState: StateEnum, input: String): StateEnum = currentState match {
    case Init => input match {
      case "start" => Active
      case _       => Init
    }
    case Active => input match {
      case "pause" => Paused
      case "stop"  => Ended
      case _       => Active
    }
    case Paused => input match {
      case "resume" => Active
      case "stop"   => Ended
      case _        => Paused
    }
    case Ended =>
      // No transitions from Ended state
      Ended
  }
}


object SimpleStateMachineDemo extends App {
  import States.StateEnum
  var currentState: StateEnum = States.Init

  def processInput(input: String): Unit = {
    println(s"Current state: $currentState")
    currentState = States.nextState(currentState, input)
    println(s"Transitioned to: $currentState")
  }

  processInput("start")
  processInput("pause")
  processInput("resume")
  processInput("stop")
}
