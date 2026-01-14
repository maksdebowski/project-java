package example.domain.game;

import java.util.concurrent.ThreadLocalRandom;

public final class SimpleCave implements Cave {
    public final int columns;
    public final int rows;
    public boolean[] rocks;

    private SimpleCave() {
        this.columns = 0;
        this.rows = 0;
        this.rocks = new boolean[0];
    }

    public SimpleCave(int rows, int columns) {
        this.columns = columns;
        this.rows = rows;
        this.rocks = new boolean[columns * rows];
        initialize();
        for (int i = 0; i < 5; i++) {
            iterate();
        }
        border();
    }

    private void border() {
        for (int row = 0; row < rows(); row++) {
            set(row, 0, true);
            set(row, columns() - 1, true);
        }
        for (int column = 0; column < columns(); column++) {
            set(0, column, true);
            set(rows() - 1, column, true);
        }
    }

    private void initialize() {
        final var rg = ThreadLocalRandom.current();
        for (int row = 0; row < rows(); row++) {
            for (int column = 0; column < columns(); column++) {
                if (0 < column && column < columns() - 1 && 0 < row && row < rows() - 1) {
                    set(row, column, rg.nextFloat() > 0.65);
                } else {
                    set(row, column, true);
                }
            }
        }
    }

    private int neighbours(int j, int i) {
        return rock1(j - 1, i - 1) + rock1(j, i - 1) + rock1(j + 1, i - 1) +
                rock1(j - 1, i) + rock1(j + 1, i) +
                rock1(j - 1, i + 1) + rock1(j, i + 1) + rock1(j + 1, i + 1);
    }

    private void iterate() {
        final var next = new boolean[columns * rows];

        for (int j = 1; j < rows - 1; j++) {
            for (int i = 1; i < columns - 1; i++) {
                final var rocks = neighbours(j, i);
                if (!rock(j, i) && rocks <= 4) { // passage
                    next[j * columns + i] = false;
                } else if (rock(j, i) && rocks <= 2) {
                    next[j * columns + i] = false;
                } else {
                    next[j * columns + i] = true;
                }
            }
        }

        rocks = next;
    }

    private int rock1(int row, int column) {
        return rocks[row * columns + column] ? 1 : 0;
    }

    public boolean rock(int row, int column) {
        return rocks[row * columns + column];
    }

    public void set(int row, int column, boolean value) {
        rocks[row * columns + column] = value;
    }

    public int rows() {
        return this.rows;
    }

    public int columns() {
        return this.columns;
    }
}

