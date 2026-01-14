package example.domain.game;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Player.HumanPlayer.class, name = "P"),
        @JsonSubTypes.Type(value = Player.Dragon.class, name = "D"),
})
public sealed interface Player {
    record Dragon(Dragon.Size size) implements Player {
        public enum Size {
            Small,
            Medium,
            Large
        }
    }

    record HumanPlayer(String name) implements Player {
    }
}

