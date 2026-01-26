package example.npc;

import example.domain.game.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DragonAI {
    private final Random random = new Random();
    
    public List<Action> generateDragonActions(Map<Player, Location> playerLocations, Cave cave) {
        List<Action> actions = new ArrayList<>();
        
        for (Map.Entry<Player, Location> entry : playerLocations.entrySet()) {
            if (entry.getKey() instanceof Player.Dragon dragon) {
                Direction direction = chooseDragonDirection(entry.getValue(), playerLocations, cave, dragon);
                actions.add(new Action(dragon, direction));
            }
        }
        
        return actions;
    }
    
    private Direction chooseDragonDirection(Location dragonLocation, 
                                           Map<Player, Location> playerLocations, 
                                           Cave cave,
                                           Player.Dragon dragon) {
        // Find nearest human player
        Player.HumanPlayer nearestHuman = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Map.Entry<Player, Location> entry : playerLocations.entrySet()) {
            if (entry.getKey() instanceof Player.HumanPlayer human) {
                double distance = calculateDistance(dragonLocation, entry.getValue());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestHuman = human;
                }
            }
        }
        
        // If there's a nearby human, move towards them
        if (nearestHuman != null) {
            Location humanLocation = playerLocations.get(nearestHuman);
            Direction towardsHuman = getDirectionTowards(dragonLocation, humanLocation, cave);
            if (towardsHuman != null) {
                return towardsHuman;
            }
        }
        
        // Otherwise, move randomly
        return getRandomValidDirection(dragonLocation, cave);
    }
    
    private Direction getDirectionTowards(Location from, Location to, Cave cave) {
        int rowDiff = to.row() - from.row();
        int colDiff = to.column() - from.column();
        
        List<Direction> preferredDirections = new ArrayList<>();
        
        // Prioritize the larger difference
        if (Math.abs(rowDiff) > Math.abs(colDiff)) {
            if (rowDiff > 0) preferredDirections.add(Direction.Down);
            else if (rowDiff < 0) preferredDirections.add(Direction.Up);
            
            if (colDiff > 0) preferredDirections.add(Direction.Right);
            else if (colDiff < 0) preferredDirections.add(Direction.Left);
        } else {
            if (colDiff > 0) preferredDirections.add(Direction.Right);
            else if (colDiff < 0) preferredDirections.add(Direction.Left);
            
            if (rowDiff > 0) preferredDirections.add(Direction.Down);
            else if (rowDiff < 0) preferredDirections.add(Direction.Up);
        }
        
        // Try preferred directions
        for (Direction dir : preferredDirections) {
            if (isValidMove(from, dir, cave)) {
                return dir;
            }
        }
        
        return null;
    }
    
    private Direction getRandomValidDirection(Location location, Cave cave) {
        List<Direction> validDirections = new ArrayList<>();
        
        for (Direction dir : Direction.values()) {
            if (isValidMove(location, dir, cave)) {
                validDirections.add(dir);
            }
        }
        
        if (validDirections.isEmpty()) {
            return Direction.Up; // Fallback
        }
        
        return validDirections.get(random.nextInt(validDirections.size()));
    }
    
    private boolean isValidMove(Location location, Direction direction, Cave cave) {
        Location newLocation = getNewLocation(location, direction);
        
        // Check bounds
        if (newLocation.row() < 0 || newLocation.row() >= cave.rows() ||
            newLocation.column() < 0 || newLocation.column() >= cave.columns()) {
            return false;
        }
        
        // Check for rock
        return !cave.rock(newLocation.row(), newLocation.column());
    }
    
    private Location getNewLocation(Location location, Direction direction) {
        return switch (direction) {
            case Up -> new Location(location.row() - 1, location.column());
            case Down -> new Location(location.row() + 1, location.column());
            case Left -> new Location(location.row(), location.column() - 1);
            case Right -> new Location(location.row(), location.column() + 1);
        };
    }
    
    private double calculateDistance(Location from, Location to) {
        int rowDiff = from.row() - to.row();
        int colDiff = from.column() - to.column();
        return Math.sqrt(rowDiff * rowDiff + colDiff * colDiff);
    }
}
