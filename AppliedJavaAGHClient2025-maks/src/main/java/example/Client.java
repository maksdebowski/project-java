package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.config.GameConfig;
import example.domain.Request;
import example.domain.Response;
import example.domain.game.Direction;
import example.domain.game.Player;
import example.game.GameState;
import example.renderer.CaveRenderer;
import example.strategy.MovementStrategy;
import example.strategy.SmartMovementStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import example.config.GameConfig;

import java.io.*;
import java.net.Socket;

public class Client {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    
    private final GameConfig config;
    private final GameState gameState;
    private final MovementStrategy movementStrategy;
    private final CaveRenderer renderer;
    
    public Client() {
        this.config = new GameConfig();
        this.gameState = new GameState();
        this.movementStrategy = new SmartMovementStrategy();
        this.renderer = new CaveRenderer();
    }

    public static void main(String[] args) {
        try {
            new Client().startClient();
        } catch (Exception e) {
            logger.error("Fatal error in client", e);
            System.exit(1);
        }
    }

    public void startClient() {
        final String host = config.getServerHost();
        final int port = config.getServerPort();
        final String playerKey = config.getPlayerKey();
        final String playerName = config.getPlayerName();
        
        logger.info("Starting client for player: {}", playerName);
        logger.info("Connecting to server at {}:{}", host, port);
        
        try (final var socket = new Socket(host, port);
             final var is = socket.getInputStream();
             final var isr = new InputStreamReader(is);
             final var reader = new BufferedReader(isr);
             final var os = socket.getOutputStream();
             final var osr = new OutputStreamWriter(os);
             final var writer = new BufferedWriter(osr)) {
            
            logger.info("Connected to server successfully");
            
            // Send authorization
            sendAuthorization(writer, playerKey);
            
            // Main game loop
            while (!Thread.currentThread().isInterrupted()) {
                final var line = reader.readLine();
                if (line == null) {
                    logger.info("Server closed connection");
                    break;
                }
                
                final var response = objectMapper.readValue(line, Response.class);
                handleResponse(response, writer, playerName);
            }
        } catch (IOException e) {
            logger.error("Error in client operation", e);
        } finally {
            logger.info("Client exiting");
        }
    }
    
    private void sendAuthorization(BufferedWriter writer, String key) throws IOException {
        final var authRequest = new Request.Authorize(key);
        final var json = objectMapper.writeValueAsString(authRequest);
        writer.write(json);
        writer.newLine();
        writer.flush();
        logger.info("Sent authorization request");
    }
    
    private void handleResponse(Response response, BufferedWriter writer, String playerName) throws IOException {
        switch (response) {
            case Response.Authorized authorized -> handleAuthorized(authorized);
            case Response.Unauthorized unauthorized -> handleUnauthorized();
            case Response.StateCave stateCave -> handleStateCave(stateCave);
            case Response.StateLocations stateLocations -> handleStateLocations(stateLocations, writer, playerName);
        }
    }
    
    private void handleAuthorized(Response.Authorized authorized) {
        gameState.setPlayer(authorized.humanPlayer());
        logger.info("Authorization successful for player: {}", authorized.humanPlayer().name());
    }
    
    private void handleUnauthorized() {
        logger.error("Authorization failed - invalid key");
        throw new RuntimeException("Unauthorized - check your player key in config.properties");
    }
    
    private void handleStateCave(Response.StateCave stateCave) {
        gameState.setCave(stateCave.cave());
        logger.info("Received cave map: {} rows x {} columns", 
                   stateCave.cave().rows(), 
                   stateCave.cave().columns());
    }
    
    private void handleStateLocations(Response.StateLocations stateLocations, 
                                     BufferedWriter writer, 
                                     String playerName) throws IOException {
        // Update game state
        gameState.updateFromStateLocations(stateLocations, playerName);
        
        // Render the cave
        renderer.renderCave(
            gameState.getCave(),
            gameState.getItemLocations(),
            gameState.getPlayerLocations(),
            playerName,
            gameState.getHealth(),
            gameState.getGold()
        );
        
        // Get next move from strategy
        Direction nextMove = movementStrategy.getNextMove(
            gameState.getCave(),
            gameState.getCurrentLocation(),
            gameState.getItemLocations(),
            gameState.getPlayerLocations()
        );
        
        // Send command
        sendCommand(writer, nextMove);
    }
    
    private void sendCommand(BufferedWriter writer, Direction direction) throws IOException {
        final var command = new Request.Command(direction);
        final var json = objectMapper.writeValueAsString(command);
        writer.write(json);
        writer.newLine();
        writer.flush();
        logger.debug("Sent command: {}", direction);
    }
}