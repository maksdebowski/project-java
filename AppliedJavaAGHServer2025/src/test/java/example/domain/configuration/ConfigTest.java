package example.domain.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.domain.Request;
import example.domain.game.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void mapper() throws JsonProcessingException {
        final var known = List.of(
                new PlayerConfiguration(new Request.Authorize("1234"), new Player.HumanPlayer("Player0"))
        );

        final var actual = objectMapper.readValue("{\"known\":[{\"authorize\":{\"type\":\"A\",\"key\":\"1234\"},\"player\":{\"type\":\"P\",\"name\":\"Player0\"}}]}", Config.class);
        final var expected = new Config(known);
        Assertions.assertEquals(expected, actual);
    }
}