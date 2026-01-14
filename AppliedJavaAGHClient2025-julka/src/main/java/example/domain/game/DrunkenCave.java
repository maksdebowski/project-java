package example.domain.game;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class DrunkenCave implements Cave {
    public final int columns;
    public final int rows;
    public boolean[] rocks;

    private DrunkenCave() {
        this.columns = 0;
        this.rows = 0;
        this.rocks = new boolean[0];
    }

    public DrunkenCave(int rows, int columns) {
        this.columns = columns;
        this.rows = rows;
        this.rocks = new boolean[columns * rows];
        Arrays.fill(this.rocks, true);
        initialize();
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
        var row = rows() / 2;
        var column = columns() / 2;
        var n = (rows() * columns() * 8) / 10;
        while (n > 0) {
            if (rock(row, column)) {
                set(row, column, false);
                n--;
            }

            switch (rg.nextInt(4)) {
                case 0:
                    row--;
                    break;
                case 1:
                    row++;
                    break;
                case 2:
                    column--;
                    break;
                case 3:
                    column++;
                    break;
            }

            if (row < 0) {
                row = 0;
            }

            if (column < 0) {
                column = 0;
            }

            if (row == rows()) {
                row = rows() - 1;
            }

            if (column == columns()) {
                column = columns() - 1;
            }
        }
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

