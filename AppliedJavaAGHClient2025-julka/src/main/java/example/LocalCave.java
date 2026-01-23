package example;

public class LocalCave {
    private final int rows;
    private final int columns;
    private final boolean[][] rocks;

    public LocalCave(int rows, int columns, boolean[][] rocks) {
        this.rows = rows;
        this.columns = columns;
        this.rocks = rocks;
    }

    public boolean rock(int row, int col) {
        return rocks[row][col];
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }
}
