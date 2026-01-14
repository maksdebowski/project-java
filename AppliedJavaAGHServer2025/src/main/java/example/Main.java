package example;

import example.domain.game.Cave;
import example.domain.game.DrunkenCave;
import example.domain.game.SimpleCave;
import example.game.Game;
import example.server.Server;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
//        final var cave = new SimpleCave(60, 160);
        final var cave = new DrunkenCave(15, 50);
        final var game = new Game(cave);
        //final var server = new Server(game, "{\"known\":[{\"authorize\":{\"type\":\"A\",\"key\":\"1234\"},\"player\":{\"type\":\"P\",\"name\":\"Player0\"}}]}");
        final var server = new Server(game, Path.of("config/configuration.json"));
        server.start(8080);
    }
}
