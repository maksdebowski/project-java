package example.domain.game;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleCave.class, name = "S"),
        @JsonSubTypes.Type(value = DrunkenCave.class, name = "DC"),
})
public interface Cave {
    boolean rock(int row, int column);
    int rows();
    int columns();
}
