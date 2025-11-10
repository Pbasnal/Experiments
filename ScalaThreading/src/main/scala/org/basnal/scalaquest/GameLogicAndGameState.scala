package org.basnal.scala
package scalaquest

object GameLogicAndGameState extends App {

  case class Player(name: String, hp: Int, inventory: List[String])

  case class GameState(player: Player, log: List[String])

  object WithCats {

    import cats.data.State
    import cats.syntax.all._

    type Game[A] = State[GameState, A]

    def takeDamage(amount: Int): Game[Unit] = State.modify { state =>
      val newHp = (state.player.hp - amount).max(0)
      state.copy(
        player = state.player.copy(hp = newHp),
        log = s"Player took damage $amount" :: state.log
      )
    }

    def addItem(item: String): Game[Unit] = State.modify { state =>
      state.copy(
        player = state.player.copy(inventory = item :: state.player.inventory),
        log = s"Player added $item to inventory" :: state.log
      )
    }

    def currentHp: Game[Int] = State.inspect(_.player.hp)


    val gameLogic: Game[Unit] = for {
      _ <- addItem("Iron Sword")
      _ <- takeDamage(7)
      _ <- addItem("Health Potion")
      _ <- takeDamage(20)
    } yield ()

    def runGame(): Unit = {
      val initialState = GameState(Player("Arjun", 100, Nil), Nil)

      val (finalState, _) = gameLogic.run(initialState).value

      finalState.log.reverse.foreach(println)
    }
  }

  WithCats.runGame()


  object CustomState {
    case class MyState[S, A](run: S => (A, S)) {
      def map[B](f: A => B): MyState[S, B] = {
        MyState(s => {
          val (a, state) = run(s)
          (f(a), state)
        })
      }

      def flatMap[B](f: A => MyState[S, B]): MyState[S, B] =
        MyState(s => {
          val (a, intermediateState) = run(s)
          f(a).run(intermediateState)
        })
    }

    type Game[A] = MyState[GameState, A]

    def takeDamage(amount: Int): Game[Unit] =
      MyState(state => {
        val newHp = (state.player.hp - amount).max(0)
        val updatedPlayer = state.player.copy(hp = newHp)
        val updatedLog = s"Player took $amount damage!" :: state.log
        ((), state.copy(player = updatedPlayer, log = updatedLog))
      })

    def addItem(item: String): Game[Unit] =
      MyState { state =>
        val updatedPlayer = state.player.copy(inventory = item :: state.player.inventory)
        val updatedLog = s"Player picked up $item." :: state.log
        ((), state.copy(player = updatedPlayer, log = updatedLog))
      }

    val gameLogic: Game[Unit] = for {
      _ <- addItem("Iron Sword")
      _ <- takeDamage(7)
      _ <- addItem("Health Potion")
      _ <- takeDamage(20)
    } yield ()

    val initial = GameState(Player("Zara", 50, Nil), Nil)
    val ((), finalState) = gameLogic.run(initial)


    finalState.log.reverse.foreach(println)
//
//    val finalState2 = takeDamage(20).flatMap { _ =>
//      addItem("potion").flatMap { _ =>
//        takeDamage(7).flatMap { _ =>
//          addItem("Sword").map(_ => _)
//        }
//      }
//    }

//    finalState2.run(initial)._2
  }
}
