package example.domain.game;

public record Location(int row, int column) {
    public double distanceFromOrigin() {
        return Math.abs(row) + Math.abs(column);
    }

    public boolean equals(Location loc) {
        if (row == loc.row && column == loc.column) {
            return true;
        }
        return false;
    }
}
