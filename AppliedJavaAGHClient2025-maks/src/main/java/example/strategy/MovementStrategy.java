package example.strategy;

import example.domain.Response;
import example.domain.game.Cave;
import example.domain.game.Direction;
import example.domain.game.Location;

import java.util.Collection;

public interface MovementStrategy {
    Direction getNextMove(Cave cave, 
                         Location currentLocation,
                         Collection<Response.StateLocations.ItemLocation> itemLocations,
                         Collection<Response.StateLocations.PlayerLocation> playerLocations);
}
