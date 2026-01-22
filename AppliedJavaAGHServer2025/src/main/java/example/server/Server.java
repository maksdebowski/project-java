package example.server;

// Server.java

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import example.domain.Request;
import example.domain.Response;
import example.domain.configuration.Config;
import example.domain.configuration.PlayerConfiguration;
import example.domain.game.Action;
import example.domain.game.Direction;
import example.domain.game.Player;
import example.game.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<State> state = new AtomicReference<>(new State(List.of(), List.of(), Map.of(), Map.of()));
    private final BlockingQueue<Action> actionsQueue = new LinkedBlockingQueue<>();
    private final Lock stateLock = new ReentrantLock();
    private final Condition stateUpdated = stateLock.newCondition();
    private final Game game;
    private final Collection<PlayerConfiguration> known;

    public Server(Game game, Path path) throws IOException {
        final var config = objectMapper.readValue(Files.readAllBytes(path), Config.class);
        this.known = config.known();
        this.game = game;
        known.forEach((configuration) -> game.add(configuration.player(), game::randomLocation));
    }

    /**
     * @param gamePort Port dla logiki gry (TCP/JSON)
     * @param httpPort Port dla podglądu stanu (WWW)
     */
    public void start(int gamePort, int httpPort) {
        startHttpServer(httpPort);

        final var threadProcessCommand = Executors.defaultThreadFactory().newThread(this::processCommands);
        threadProcessCommand.start();

        try (final var serverSocket = new ServerSocket(gamePort)) {
            logger.info("Game server started on port {}", gamePort);
            logger.info("Web status view available at http://localhost:{}", httpPort);

            while (!Thread.currentThread().isInterrupted()) {
                final var clientSocket = serverSocket.accept();
                Thread.startVirtualThread(() -> handleClientConnection(clientSocket));
            }
        } catch (IOException e) {
            logger.error("Server error", e);
        } finally {
            threadProcessCommand.interrupt();
        }
    }

    private void startHttpServer(int port) {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // Endpoint zwracający aktualny stan jako JSON
            httpServer.createContext("/state", exchange -> {
                byte[] response = objectMapper.writeValueAsBytes(state.get());
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });

            httpServer.createContext("/", exchange -> {
                String mapContent = game.renderString();
                String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset='UTF-8'>
            <meta http-equiv='refresh' content='1'>
            <style>
                body { 
                    background-color: #121212; 
                    color: #ffffff; 
                    font-family: 'Courier New', monospace; 
                    display: flex; 
                    flex-direction: column; 
                    align-items: center; 
                    padding-top: 20px;
                }
                .map-grid { 
                    background-color: #1e1e1e; 
                    padding: 10px; 
                    border-radius: 8px; 
                    box-shadow: 0 4px 15px rgba(0,0,0,0.5);
                    line-height: 0;
                }
                h1 { color: #f1c40f; }
            </style>
        </head>
        <body>
            <h1>Dragon Cave - Live Map</h1>
            <div class='map-grid'>
                %s
            </div>
            <p>Players: %d | Items: %d</p>
            <p>Healths: %s</p>
        </body>
        </html>
        """.formatted(
                        mapContent,
                        state.get().playerLocations().size(),
                        state.get().itemLocations().size(),
                        state.get().playerHealths.toString()
                );
                byte[] responseBytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            });

            httpServer.setExecutor(null); // używa domyślnego
            httpServer.start();
        } catch (IOException e) {
            logger.error("Failed to start HTTP server", e);
        }
    }

    private void handleClientCommands(BufferedReader reader, Player.HumanPlayer player) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final var line = reader.readLine();
                if (line == null) break;

                // WALIDACJA: Odporność na błędny JSON
                try {
                    final var request = objectMapper.readValue(line, Request.class);
                    if (request instanceof Request.Command(Direction direction)) {
                        if (direction != null) {
                            actionsQueue.put(new Action(player, direction));
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Received invalid JSON from player {}: {}", player, line);
                    // Nie przerywamy pętli, czekamy na kolejną komendę
                }
            }
        } catch (IOException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (final var is = clientSocket.getInputStream();
             final var isr = new InputStreamReader(is);
             final var reader = new BufferedReader(isr);
             final var os = clientSocket.getOutputStream();
             final var osr = new OutputStreamWriter(os);
             final var writer = new BufferedWriter(osr)) {
            // handle authorization
            final var line = reader.readLine();
            if (line == null) {
                return;
            }

            final Player.HumanPlayer player;
            final var request = objectMapper.readValue(line, Request.class);
            if (Objects.requireNonNull(request) instanceof Request.Authorize authorize) {
                player = known.stream()
                        .filter(configuration -> configuration.authorize().equals(authorize))
                        .findAny()
                        .map(PlayerConfiguration::player)
                        .orElse(null);
                if (player == null) {
                    final var json = objectMapper.writeValueAsString(new Response.Unauthorized());
                    writer.write(json);
                    writer.newLine();
                    writer.flush();
                    return;
                }

                final var json = objectMapper.writeValueAsString(new Response.Authorized(player));
                writer.write(json);
                writer.newLine();
                writer.flush();
            } else {
                return;
            }

            {
                final var json = objectMapper.writeValueAsString(new Response.StateCave(game.cave()));
                writer.write(json);
                writer.newLine();
                writer.flush();
            }

            Thread t1 = Thread.startVirtualThread(() -> handleClientCommands(reader, player));
            Thread t2 = Thread.startVirtualThread(() -> handleClientState(writer, player));
            t1.join();
            t2.join();
        } catch (IOException e) {
            logger.error("Commands processing thread interrupted", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            logger.error("Commands processing thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void processCommands() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Wait for one second
                Thread.sleep(1000);

                // Process all collected commands
                final var actions = new LinkedList<Action>();
                actionsQueue.drainTo(actions);

                game.step(actions);

                final var itemLocations = game.itemLocation().entrySet().stream().map(entry -> new Response.StateLocations.ItemLocation(entry.getKey(), entry.getValue())).toList();
                final var playerLocations = game.playerLocation().entrySet().stream().map(entry -> new Response.StateLocations.PlayerLocation(entry.getKey(), entry.getValue())).toList();

                // Update the state
                stateLock.lock();
                try {
                    state.set(new State(itemLocations, playerLocations, game.playerHealth(), game.playerGold()));
                    // Notify client state threads
                    stateUpdated.signalAll();
                } finally {
                    stateLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.error("Commands processing thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }


    private void handleClientState(BufferedWriter writer, Player.HumanPlayer player) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                stateLock.lock();
                try {
                    stateUpdated.await();
                    // Send the new state to the client
                    final var currentState = state.get();
                    final var playerState = new Response.StateLocations(currentState.itemLocations(),
                            currentState.playerLocations(),
                            currentState.playerHealths().getOrDefault(player, 0),
                            currentState.playerGolds().getOrDefault(player, 0));
                    final var stateJson = objectMapper.writeValueAsString(playerState);
                    writer.write(stateJson);
                    writer.newLine();
                    writer.flush();
                } finally {
                    stateLock.unlock();
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
    }

    private record State(List<Response.StateLocations.ItemLocation> itemLocations,
                         List<Response.StateLocations.PlayerLocation> playerLocations,
                         Map<Player, Integer> playerHealths,
                         Map<Player, Integer> playerGolds) {
    }
}
