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
        Assertions.assertEquals(89, game.health(player1));
        Assertions.assertEquals(89, game.health(player2));
    }

    @Test
    public void onlyOnePlayerMoves() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        final var player2 = new Player.HumanPlayer("2");
        final var health = new Item.Health(0, 10);

        game.add(player1, () -> new Location(1, 1));
        game.add(player2, () -> new Location(4, 1));
        game.add(health, () -> new Location(2, 1));

        final var actions = List.of(new Action(player1, Direction.Down));

        final var expected = Map.of(player1, new Location(3, 1), player2, new Location(4, 1));

        game.step(actions);
        game.step(actions);

        Assertions.assertEquals(expected, game.playerLocation());
        Assertions.assertEquals(99, game.health(player1));
        Assertions.assertEquals(100, game.health(player2));
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

    @Test
    public void playerCannotMoveThroughRock() {
        final var cave = new SimpleCave(10, 10);
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        game.add(player1, () -> new Location(5, 5));

        final var initialLocation = game.playerLocation().get(player1);
        final var initialHealth = game.health(player1);

        for (Direction dir : Direction.values()) {
            final var nextLoc = switch (dir) {
                case Up -> new Location(initialLocation.row() - 1, initialLocation.column());
                case Down -> new Location(initialLocation.row() + 1, initialLocation.column());
                case Left -> new Location(initialLocation.row(), initialLocation.column() - 1);
                case Right -> new Location(initialLocation.row(), initialLocation.column() + 1);
            };

            if (cave.rock(nextLoc.row(), nextLoc.column())) {
                game.step(List.of(new Action(player1, dir)));
                Assertions.assertEquals(initialLocation, game.playerLocation().get(player1));
                Assertions.assertTrue(game.health(player1) < initialHealth);
                return;
            }
        }
    }

    @Test
    public void playerCannotMoveOutOfBounds() {
        final var cave = new SimpleCave(10, 10);
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        game.add(player1, () -> new Location(0, 0));

        final var initialHealth = game.health(player1);
        game.step(List.of(new Action(player1, Direction.Up)));

        Assertions.assertEquals(new Location(0, 0), game.playerLocation().get(player1));
        Assertions.assertEquals(initialHealth - 6, game.health(player1));

        game.step(List.of(new Action(player1, Direction.Left)));
        Assertions.assertEquals(new Location(0, 0), game.playerLocation().get(player1));
        Assertions.assertEquals(initialHealth - 12, game.health(player1));
    }

    @Test
    public void itemGenerationMaintainsCount() {
        final var cave = new SimpleCave(20, 20);
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        game.add(player1, () -> new Location(5, 5));

        for (int i = 0; i < 5; i++) {
            game.add(new Item.Gold(i, 10), game::randomLocation);
        }

        long initialGoldCount = game.itemLocation().keySet().stream()
                .filter(item -> item instanceof Item.Gold)
                .count();

        game.step(List.of(new Action(player1, Direction.Down)));

        long afterStepGoldCount = game.itemLocation().keySet().stream()
                .filter(item -> item instanceof Item.Gold)
                .count();

        Assertions.assertTrue(afterStepGoldCount >= 5);
    }

    @Test
    public void lootDistributionBetweenPlayers() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("A");
        final var player2 = new Player.HumanPlayer("B");
        final var gold = new Item.Gold(0, 100);

        game.add(player1, () -> new Location(1, 1));
        game.add(player2, () -> new Location(1, 3));
        game.add(gold, () -> new Location(1, 2));

        final var actions = List.of(
                new Action(player1, Direction.Right),
                new Action(player2, Direction.Left)
        );
        game.step(actions);

        int totalGold = game.gold(player1) + game.gold(player2);
        Assertions.assertEquals(100, totalGold);
        Assertions.assertTrue(game.gold(player1) > 0);
        Assertions.assertTrue(game.gold(player2) > 0);
    }

    @Test
    public void healthCannotExceed100() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        final var health1 = new Item.Health(0, 50);
        final var health2 = new Item.Health(1, 50);

        game.add(player1, () -> new Location(1, 1));
        game.add(health1, () -> new Location(2, 1));
        game.add(health2, () -> new Location(3, 1));

        game.step(List.of(new Action(player1, Direction.Down)));
        Assertions.assertEquals(100, game.health(player1));

        game.step(List.of(new Action(player1, Direction.Down)));
        Assertions.assertEquals(100, game.health(player1));
    }

//    @Test
//    public void goldCannotExceed100() {
//        final var cave = new EmptyCave();
//        final var game = new Game(cave);
//
//        final var player1 = new Player.HumanPlayer("1");
//        final var gold1 = new Item.Gold(0, 60);
//        final var gold2 = new Item.Gold(1, 60);
//
//        game.add(player1, () -> new Location(1, 1));
//        game.add(gold1, () -> new Location(2, 1));
//        game.add(gold2, () -> new Location(3, 1));
//
//        game.step(List.of(new Action(player1, Direction.Down)));
//        Assertions.assertEquals(60, game.gold(player1));
//
//        game.step(List.of(new Action(player1, Direction.Down)));
//        Assertions.assertEquals(100, game.gold(player1));
//    }

    @Test
    public void playerHealthDecreasesWithMovement() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        game.add(player1, () -> new Location(5, 5));

        Assertions.assertEquals(100, game.health(player1));

        game.step(List.of(new Action(player1, Direction.Down)));
        Assertions.assertEquals(99, game.health(player1));

        game.step(List.of(new Action(player1, Direction.Down)));
        Assertions.assertEquals(98, game.health(player1));
    }

    @Test
    public void playerHealthCannotGoBelowZero() {
        final var cave = new EmptyCave();
        final var game = new Game(cave);

        final var player1 = new Player.HumanPlayer("1");
        final var player2 = new Player.HumanPlayer("2");

        game.add(player1, () -> new Location(1, 1));
        game.add(player2, () -> new Location(1, 3));

        for (int i = 0; i < 20; i++) {
            game.step(List.of(
                    new Action(player1, Direction.Right),
                    new Action(player2, Direction.Left)
            ));
        }

        Assertions.assertTrue(game.health(player1) >= 0);
        Assertions.assertTrue(game.health(player2) >= 0);
    }

}