package example.domain.configuration;

import example.domain.Request;
import example.domain.game.Player;

public record PlayerConfiguration(Request.Authorize authorize, Player.HumanPlayer player) {
}
