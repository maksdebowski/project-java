package example;

import example.domain.game.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MoveStrategy {

    private static final Random random = new Random();

    public static Direction chooseDirection(LocalCave cave, int row, int col) {
        List<Direction> possible = new ArrayList<>();

        if (!cave.rock(row - 1, col)) possible.add(Direction.Up);
        if (!cave.rock(row + 1, col)) possible.add(Direction.Down);
        if (!cave.rock(row, col - 1)) possible.add(Direction.Left);
        if (!cave.rock(row, col + 1)) possible.add(Direction.Right);

        if (possible.isEmpty()) {
            return Direction.Up; // fallback
        }

        return possible.get(random.nextInt(possible.size()));
    }
}
