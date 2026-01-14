package example.domain.game;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Item.Gold.class, name = "G"),
        @JsonSubTypes.Type(value = Item.Health.class, name = "H"),
})
public sealed interface Item {
    record Gold(int id, int value) implements Item {
    }

    record Health(int id, int value) implements Item {
    }
}
