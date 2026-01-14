package example.game;

import example.domain.game.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GameTest {
    private static class EmptyCave implements Cave {
        @Override
        public boolean rock(int row, int column) {
            return false;
        }

        @Override
        public int rows() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int columns() {
            return Integer.MAX_VALUE;
        }
    }

    @Test
    public void basic() {
        final var cave = new SimpleCave(20, 20);
        final var game = new Game(cave);

        for (final var s : Arrays.asList("Player 0", "Player 1", "Player 2")) {
            game.add(new Player.HumanPlayer(s), game::randomLocation);
        }

        for (int i = 1; i < 6; i++) {
            game.add(new Item.Gold(i, 10), game::randomLocation);
        }
        game.render();
    }

    @Test
    public void twoPlayersFight() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        final var player2 = new Player.HumanPlayer("2");

        game.add(player1, () -> new Location(1, 1));
        game.add(player2, () -> new Location(3, 1));

        final var actions = List.of(new Action(player1, Direction.Down), new Action(player2, Direction.Up));
        final var expected = Map.of(player1, new Location(2, 1), player2, new Location(2, 1));
        game.step(actions);

        Assertions.assertEquals(expected, game.playerLocation());
        Assertions.assertEquals(50, game.health(player1));
        Assertions.assertEquals(50, game.health(player2));
    }

    @Test
    public void onlyOnePlayerMoves() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        final var player2 = new Player.HumanPlayer("2");
        final var health = new Item.Health(0, 10);

        game.add(player1, () -> new Location(1, 1));
        game.add(player2, () -> new Location(3, 1));
        game.add(health, () -> new Location(2, 1));

        final var actions = List.of(new Action(player1, Direction.Down));

        final var expected = Map.of(player1, new Location(3, 1), player2, new Location(3, 1));

        game.step(actions);
        game.step(actions);

        Assertions.assertEquals(expected, game.playerLocation());
        Assertions.assertEquals(60, game.health(player1));
        Assertions.assertEquals(50, game.health(player2));
    }

    @Test
    public void playerMoves() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");

        game.add(player1, () -> new Location(1, 1));

        final var actions = List.of(new Action(player1, Direction.Down));
        final var expected = Map.of(player1, new Location(2, 1));
        game.step(actions);

        Assertions.assertEquals(expected, game.playerLocation());
    }

    @Test
    public void playerGetsGold() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        final var gold1 = new Item.Gold(0, 10);

        game.add(player1, () -> new Location(1, 1));
        game.add(gold1, () -> new Location(2, 1));

        final var actions = List.of(new Action(player1, Direction.Down));
        final var expected = Map.of(player1, new Location(2, 1));
        game.step(actions);

        Assertions.assertEquals(expected, game.playerLocation());
        Assertions.assertEquals(gold1.value(), game.gold(player1));
    }

}