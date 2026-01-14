package example.domain.game;

public record Location(int row, int column) {
    public double distanceFromOrigin() {
        return Math.abs(row) + Math.abs(column);
    }
}
