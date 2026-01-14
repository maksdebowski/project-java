package example.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import example.domain.game.Item;
import example.domain.game.Location;
import example.domain.game.Cave;
import example.domain.game.Player;

import java.util.Collection;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Response.StateCave.class, name = "C"),
        @JsonSubTypes.Type(value = Response.StateLocations.class, name = "L"),
        @JsonSubTypes.Type(value = Response.Authorized.class, name = "A"),
        @JsonSubTypes.Type(value = Response.Unauthorized.class, name = "U"),
})
public sealed interface Response {
    record StateCave(Cave cave) implements Response {
    }

    record StateLocations(Collection<ItemLocation> itemLocations, Collection<PlayerLocation> playerLocations, Integer health, Integer gold) implements Response {
        public record ItemLocation(Item entity, Location location) {
        }

        public record PlayerLocation(Player entity, Location location) {
        }
    }

    record Authorized(Player.HumanPlayer humanPlayer) implements Response {
    }

    record Unauthorized() implements Response {
    }
}


