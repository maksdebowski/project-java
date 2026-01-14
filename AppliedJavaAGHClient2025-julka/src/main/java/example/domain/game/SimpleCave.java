package example.domain.game;

import java.util.concurrent.ThreadLocalRandom;

public final class SimpleCave implements Cave {
    public final int columns;
    public final int rows;
    public final boolean[] rocks;

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
    }

    private void initialize() {
        final var rg = ThreadLocalRandom.current();
        for (int row = 0; row < rows(); row++) {
            for (int column = 0; column < columns(); column++) {
                set(row, column, !(0 < column && column < columns() - 1 && 0 < row && row < rows() - 1 && rg.nextFloat() < 0.8));
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

