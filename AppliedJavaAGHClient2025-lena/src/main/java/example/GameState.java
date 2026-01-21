package example;

import example.domain.Response;
import java.util.Collection;

public class GameState {

    public Collection<Response.StateLocations.PlayerLocation> playerLocations;
    public Collection<Response.StateLocations.ItemLocation> itemLocations;

    public int health;
    public int gold;

    public void update(Response.StateLocations state) {
        this.playerLocations = state.playerLocations();
        this.itemLocations = state.itemLocations();
        this.health = state.health();
        this.gold = state.gold();
    }
}
