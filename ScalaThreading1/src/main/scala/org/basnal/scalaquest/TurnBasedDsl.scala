package org.basnal.scala.scalaquest

object TurnBasedDsl extends App {
  case class Character(
                        id: String,
                        name: String,
                        hp: Int,
                        maxHp: Int,
                        isDefending: Boolean = false
                      )

  case class BattleState(
                          characters: Vector[Character],
                          turnOrder: Vector[String] // character ids
                        )

  sealed trait Action

  case class Attack(targetId: String, damage: Int) extends Action

  case class Heal(targetId: String, amount: Int) extends Action

  case object Defend extends Action

  case class StateMutation[S, A](run: S => (A, S)) {
    def map[B](f: A => B): StateMutation[S, B] =
      StateMutation(s => {
        val (a, newState) = run(s)
        (f(a), newState)
      })

    def flatMap[B](f: A => StateMutation[S, B]): StateMutation[S, B] =
      StateMutation(s => {
        val (a, intermediateState) = run(s)
        f(a).run(intermediateState)
      })
  }

  type BattleLogic[A] = StateMutation[BattleState, A]


  def applyAction(charId: String, action: Action): BattleLogic[Unit] = StateMutation { state =>
    val actor = state.characters.find(_.id == charId).get
    val updatedChars = action match {
      case Attack(targetId, dmg) =>
        state.characters.map {
          case c if c.id == targetId =>
            val effectiveDamage = if (c.isDefending) dmg / 2 else dmg
            c.copy(hp = (c.hp - effectiveDamage).max(0), isDefending = false)
          case other => other
        }

      case Heal(targetId, amt) =>
        state.characters.map {
          case c if c.id == targetId =>
            c.copy(hp = (c.hp + amt).min(c.maxHp))
          case other => other
        }

      case Defend =>
        state.characters.map {
          case c if c.id == charId => c.copy(isDefending = true)
          case other => other
        }
    }

    ((), state.copy(characters = updatedChars))
  }

  def nextTurn(): BattleLogic[Unit] = StateMutation { state =>
    val newOrder = state.turnOrder.tail :+ state.turnOrder.head
    ((), state.copy(turnOrder = newOrder))
  }

  def runTurn(action: Action): BattleLogic[Unit] = for {
    currentId <- StateMutation[BattleState, String](s => (s.turnOrder.head, s))
    _         <- applyAction(currentId, action)
    _         <- nextTurn()
  } yield ()

  val initialState = BattleState(
    characters = Vector(
      Character("c1", "Hero", 30, 30),
      Character("c2", "Goblin", 20, 20)
    ),
    turnOrder = Vector("c1", "c2")
  )

  val battleSequence = for {
    _ <- runTurn(Attack("c2", 5))
    _ <- runTurn(Defend)
    _ <- runTurn(Attack("c1", 6))
    _ <- runTurn(Heal("c1", 3))
  } yield ()

  val (result, finalState) = battleSequence.run(initialState)

  finalState.characters.foreach(println)

}
