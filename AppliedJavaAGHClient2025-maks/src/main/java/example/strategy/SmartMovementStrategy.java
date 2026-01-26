package example.strategy;

import example.domain.Response;
import example.domain.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SmartMovementStrategy implements MovementStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SmartMovementStrategy.class);
    private final Random random = new Random();
    private Location lastLocation = null;
    private Direction lastDirection = null;
    private int stuckCounter = 0;
    private static final int STUCK_THRESHOLD = 3;
    
    @Override
    public Direction getNextMove(Cave cave, 
                                Location currentLocation,
                                Collection<Response.StateLocations.ItemLocation> itemLocations,
                                Collection<Response.StateLocations.PlayerLocation> playerLocations) {
        
        // Check if stuck
        if (lastLocation != null && lastLocation.equals(currentLocation)) {
            stuckCounter++;
            logger.debug("Player stuck at location {}, counter: {}", currentLocation, stuckCounter);
        } else {
            stuckCounter = 0;
        }
        
        lastLocation = currentLocation;
        
        // If stuck, try random direction
        if (stuckCounter >= STUCK_THRESHOLD) {
            logger.info("Player stuck for {} moves, trying random direction", stuckCounter);
            return getRandomValidDirection(cave, currentLocation);
        }
        
        // Try to move towards nearest item
        Direction towardsItem = moveTowardsNearestItem(cave, currentLocation, itemLocations);
        if (towardsItem != null) {
            lastDirection = towardsItem;
            return towardsItem;
        }

        
        // Try to avoid dragons
        Direction awayFromDragon = moveAwayFromDragons(cave, currentLocation, playerLocations);
        if (awayFromDragon != null) {
            lastDirection = awayFromDragon;
            return awayFromDragon;
        }
        
        // Continue in same direction if valid
        if (lastDirection != null && isValidMove(cave, currentLocation, lastDirection)) {
            return lastDirection;
        }
        
        // Otherwise, pick random valid direction
        Direction randomDir = getRandomValidDirection(cave, currentLocation);
        lastDirection = randomDir;
        return randomDir;
    }
    
    private Direction moveTowardsNearestItem(Cave cave, Location currentLocation, 
                                            Collection<Response.StateLocations.ItemLocation> itemLocations) {
        if (itemLocations == null || itemLocations.isEmpty()) {
            return null;
        }
        
        // Find nearest item
        Response.StateLocations.ItemLocation nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Response.StateLocations.ItemLocation itemLoc : itemLocations) {
            double distance = calculateDistance(currentLocation, itemLoc.location());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = itemLoc;
            }
        }
        
        if (nearest == null) {
            return null;
        }
        
        // Try to move towards nearest item
        return getDirectionTowards(cave, currentLocation, nearest.location());
    }
    
    private Direction moveAwayFromDragons(Cave cave, Location currentLocation,
                                         Collection<Response.StateLocations.PlayerLocation> playerLocations) {
        if (playerLocations == null || playerLocations.isEmpty()) {
            return null;
        }
        
        // Find nearest dragon
        Location nearestDragon = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Response.StateLocations.PlayerLocation playerLoc : playerLocations) {
            if (playerLoc.entity() instanceof Player.Dragon) {
                double distance = calculateDistance(currentLocation, playerLoc.location());
                if (distance < minDistance && distance < 5) { // Only avoid if close
                    minDistance = distance;
                    nearestDragon = playerLoc.location();
                }
            }
        }
        
        if (nearestDragon == null) {
            return null;
        }
        
        // Try to move away from dragon
        return getDirectionAwayFrom(cave, currentLocation, nearestDragon);
    }
    
    private Direction getDirectionTowards(Cave cave, Location from, Location to) {
        int rowDiff = to.row() - from.row();
        int colDiff = to.column() - from.column();
        
        List<Direction> preferredDirections = new ArrayList<>();
        
        // Prioritize vertical movement
        if (Math.abs(rowDiff) > Math.abs(colDiff)) {
            if (rowDiff > 0) preferredDirections.add(Direction.Down);
            else if (rowDiff < 0) preferredDirections.add(Direction.Up);
            
            if (colDiff > 0) preferredDirections.add(Direction.Right);
            else if (colDiff < 0) preferredDirections.add(Direction.Left);
        } else {
            // Prioritize horizontal movement
            if (colDiff > 0) preferredDirections.add(Direction.Right);
            else if (colDiff < 0) preferredDirections.add(Direction.Left);
            
            if (rowDiff > 0) preferredDirections.add(Direction.Down);
            else if (rowDiff < 0) preferredDirections.add(Direction.Up);
        }
        
        // Try preferred directions
        for (Direction dir : preferredDirections) {
            if (isValidMove(cave, from, dir)) {
                return dir;
            }
        }
        
        return null;
    }
    
    private Direction getDirectionAwayFrom(Cave cave, Location from, Location danger) {
        int rowDiff = from.row() - danger.row();
        int colDiff = from.column() - danger.column();
        
        List<Direction> preferredDirections = new ArrayList<>();
        
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
        
        for (Direction dir : preferredDirections) {
            if (isValidMove(cave, from, dir)) {
                return dir;
            }
        }
        
        return null;
    }
    
    private Direction getRandomValidDirection(Cave cave, Location location) {
        List<Direction> validDirections = new ArrayList<>();
        
        for (Direction dir : Direction.values()) {
            if (isValidMove(cave, location, dir)) {
                validDirections.add(dir);
            }
        }
        
        if (validDirections.isEmpty()) {
            logger.warn("No valid directions available from location {}", location);
            return Direction.Up; // Fallback
        }
        
        return validDirections.get(random.nextInt(validDirections.size()));
    }
    
    private boolean isValidMove(Cave cave, Location location, Direction direction) {
        if (cave == null || location == null || direction == null) {
            return false;
        }
        
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
