package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.domain.Request;
import example.domain.Response;
import example.domain.game.*;
import example.domain.game.DrunkenCave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.util.*;

public class Client {
    // private static final String HOST = "35.208.184.138";
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        try (final var socket = new Socket(HOST, PORT);
             final var is = socket.getInputStream();
             final var isr = new InputStreamReader(is);
             final var reader = new BufferedReader(isr);
             final var os = socket.getOutputStream();
             final var osr = new OutputStreamWriter(os);
             final var writer = new BufferedWriter(osr)) {
            logger.info("Connected to server at {}:{}", HOST, PORT);

            {
                final var json = objectMapper.writeValueAsString(new Request.Authorize("Haslo_Julki"));
                writer.write(json);
                writer.newLine();
                writer.flush();
                logger.info("Sent command: {}", json);
            }

            Cave cave = null;
            Player player = null;
            LocalCave localCave = null;
            GameState gameState = new GameState();
            Collection<Response.StateLocations.ItemLocation> itemLocations = null;
            Collection<Response.StateLocations.PlayerLocation> playerLocations = null;

            Deque<Location> moves = new LinkedList<>();

            while (!Thread.currentThread().isInterrupted()) {
                final var line = reader.readLine();
                if (line == null) {
                    break;
                }

                final var response = objectMapper.readValue(line, Response.class);
                Player finalPlayer = player;
                switch (response) {
                    case Response.Authorized authorized -> {
                        player = authorized.humanPlayer();
                        logger.info("authorized: {}", authorized);
                    }
                    case Response.Unauthorized unauthorized -> {
                        logger.error("unauthorized: {}", unauthorized);
                        return;
                    }
                    case Response.StateCave stateCave -> {
                        var serverCave = stateCave.cave();

                        int rows = serverCave.rows();
                        int cols = serverCave.columns();

                        boolean[][] rocks = new boolean[rows][cols];
                        for (int r = 0; r < rows; r++) {
                            for (int c = 0; c < cols; c++) {
                                rocks[r][c] = serverCave.rock(r, c);
                            }
                        }

                        localCave = new LocalCave(rows, cols, rocks);
                        logger.info("Local cave built: {}x{}", rows, cols);
                    }
                    case Response.StateLocations stateLocations -> {
                        gameState.update(stateLocations);
                        logger.info("itemLocations: {}", itemLocations);
                        logger.info("playerLocations: {}", playerLocations);
                        logger.info(
                                "State updated: players={}, items={}, health={}, gold={}",
                                gameState.playerLocations.size(),
                                gameState.itemLocations.size(),
                                gameState.health,
                                gameState.gold
                        );
                        Direction dir = Direction.values()[new Random().nextInt(Direction.values().length)];
                        Request cmd = new Request.Command(dir);

                        if (cmd!=null) {
                            final var cmdJson = objectMapper.writeValueAsString(cmd);
                            writer.write(cmdJson);
                            writer.newLine();
                            writer.flush();
                            logger.info("Sent command: {}", cmd);
                            ConsoleRenderer.render(localCave, gameState);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error in client operation", e);
        } finally {
            logger.info("Client exiting");
        }
    }
}