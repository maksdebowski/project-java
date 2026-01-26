package example.game;

import example.domain.Response;
import example.domain.game.Cave;
import example.domain.game.Location;
import example.domain.game.Player;

import java.util.Collection;

public class GameState {
    private Cave cave;
    private Player.HumanPlayer player;
    private Location currentLocation;
    private Collection<Response.StateLocations.ItemLocation> itemLocations;
    private Collection<Response.StateLocations.PlayerLocation> playerLocations;
    private Integer health;
    private Integer gold;
    
    public Cave getCave() {
        return cave;
    }
    
    public void setCave(Cave cave) {
        this.cave = cave;
    }
    
    public Player.HumanPlayer getPlayer() {
        return player;
    }
    
    public void setPlayer(Player.HumanPlayer player) {
        this.player = player;
    }
    
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
    
    public Collection<Response.StateLocations.ItemLocation> getItemLocations() {
        return itemLocations;
    }
    
    public void setItemLocations(Collection<Response.StateLocations.ItemLocation> itemLocations) {
        this.itemLocations = itemLocations;
    }
    
    public Collection<Response.StateLocations.PlayerLocation> getPlayerLocations() {
        return playerLocations;
    }
    
    public void setPlayerLocations(Collection<Response.StateLocations.PlayerLocation> playerLocations) {
        this.playerLocations = playerLocations;
    }
    
    public Integer getHealth() {
        return health;
    }
    
    public void setHealth(Integer health) {
        this.health = health;
    }
    
    public Integer getGold() {
        return gold;
    }
    
    public void setGold(Integer gold) {
        this.gold = gold;
    }
    
    public void updateFromStateLocations(Response.StateLocations stateLocations, String playerName) {
        this.itemLocations = stateLocations.itemLocations();
        this.playerLocations = stateLocations.playerLocations();
        this.health = stateLocations.health();
        this.gold = stateLocations.gold();
        
        // Update current location
        if (playerLocations != null) {
            this.currentLocation = playerLocations.stream()
                    .filter(pl -> pl.entity() instanceof Player.HumanPlayer hp && 
                                 playerName.equals(hp.name()))
                    .map(Response.StateLocations.PlayerLocation::location)
                    .findFirst()
                    .orElse(null);
        }
    }
}
