package org.basnal.scala

import scala.annotation.tailrec
import scala.sys.exit

sealed trait Command

case class RollDice(sides: Int) extends Command

case class Attack(target: String) extends Command

case class UseItem(item: String) extends Command

case object Help extends Command

case object Exit extends Command

case object Unknown extends Command

case class GameState(log: List[String])

object CommandParser {
  def parse(input: String): Command = input.trim.toLowerCase match {
    case s"roll d$sides" if sides.forall(_.isDigit) =>
      RollDice(sides.toInt)
    case s"attack $target" => Attack(target)
    case s"use $item" => UseItem(item)
    case "exit" => Exit
    case "help" => Help
    case _ => Unknown
  }
}


object Interpreter {
  def interpret(cmd: Command, state: GameState): GameState = cmd match {
    case RollDice(sides) =>
      val roll = (math.random() * sides).toInt + 1
      state.copy(log = s"You rolled a $roll on a d$sides" :: state.log)
    case Attack(target) =>
      state.copy(log = s"You swing at the $target!" :: state.log)

    case UseItem(item) => state.copy(log = s"You used a $item." :: state.log)

    case Help =>
      state.copy(log = "Available commands: roll dX, attack <target>, use <item>" :: state.log)

    case Exit => exit()
    case Unknown =>
      state.copy(log = "I don't understand that command." :: state.log)
  }
}

object RpgCliCmds extends App {
  @tailrec
  def gameLoop(state: GameState): Unit = {
    println("[roll d[sides]] [attack target] [use [item]] [help] >")
    val input = scala.io.StdIn.readLine()
    val cmd = CommandParser.parse(input)
    val newState = Interpreter.interpret(cmd, state)
    println(newState.log.head)
    gameLoop(newState)
  }

  gameLoop(GameState(log = List("Game Started")))
}
