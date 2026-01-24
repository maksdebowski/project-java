package example.game;

import example.domain.game.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Game {
    private static final Random rg = new Random();
    private static final int NUM_GOLD = 20;
    private static final int NUM_HEALTH = 20;
    private final Map<Item, Location> itemLocation;
    private final Map<Player, Location> playerLocation;
    private final Map<Player, Integer> playerHealth;
    private final Map<Player, Integer> playerGold;
    private final Map<Player, Integer> playerMoves;
    private final Set<Player.HumanPlayer> winnersWhoReachedExit;
    private volatile boolean gameEnded = false;

    private final Cave cave;

    public Map<Player, Integer> playerHealth() {
        return Collections.unmodifiableMap(playerHealth);
    }

    public Map<Player, Integer> playerGold() {
        return Collections.unmodifiableMap(playerGold);
    }

    public Map<Player, Integer> playerMoves() { return Collections.unmodifiableMap(playerMoves);}

    public Map<Player, Location> playerLocation() {
        return Collections.unmodifiableMap(playerLocation);
    }

    public Map<Item, Location> itemLocation() {
        return Collections.unmodifiableMap(itemLocation);
    }

    public Cave cave() {
        return cave;
    }

    public Game(Cave cave) {
        this.cave = cave;
        this.playerLocation = new HashMap<>();
        this.itemLocation = new HashMap<>();
        this.playerHealth = new HashMap<>();
        this.playerGold = new HashMap<>();
        this.playerMoves = new HashMap<>();
        this.winnersWhoReachedExit = new HashSet<>();
    }

    public void render() {
        final var tbl = new String[cave.columns() * cave.rows()];
        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                if (cave.rock(row, column)) {
                    tbl[row * cave.columns() + column] = "\uD83E\uDEA8";
                } else {
                    tbl[row * cave.columns() + column] = " ";
                }
            }
        }

        for (final var entry : playerLocation.entrySet()) {
            final var location = entry.getValue();
            tbl[location.row() * cave.columns() + location.column()] = switch (entry.getKey()) {
                case Player.HumanPlayer ignored -> ignored.name().substring(0,1);
                case Player.Dragon ignored -> "\uD83D\uDC09";
            };
        }

        for (final var entry : itemLocation.entrySet()) {
            final var location = entry.getValue();
            tbl[location.row() * cave.columns() + location.column()] = switch (entry.getKey()) {
                case Item.Gold ignored -> "\uD83D\uDCB0";
                case Item.Health ignored -> "\uD83D\uDC8A";
                case Item.Exit ignored -> "\uD83D\uDEAA";
            };
        }

        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                System.out.print(tbl[row * cave.columns() + column]);
            }
            System.out.println();
        }
    }
    public String renderString() {
        StringBuilder sb = new StringBuilder();
        // Dodajemy prosty styl dla komórki: stała szerokość, wysokość i centrowanie
        String cellTemplate = "<span style='display:inline-block; width:25px; height:25px; text-align:center; vertical-align:middle;'>%s</span>";

        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                final int r = row;
                final int c = column;

                // 1. Sprawdzanie graczy
                String symbol = playerLocation.entrySet().stream()
                        .filter(e -> e.getValue().row() == r && e.getValue().column() == c)
                        .findFirst()
                        .map(e -> switch (e.getKey()) {
                            case Player.HumanPlayer p -> p.name().substring(0, 1).toUpperCase();
                            case Player.Dragon d -> "\uD83D\uDC09"; // 🐉
                        })
                        .orElse(null);

                // 2. Jeśli nie ma gracza, sprawdź przedmioty
                if (symbol == null) {
                    symbol = itemLocation.entrySet().stream()
                            .filter(e -> e.getValue().row() == r && e.getValue().column() == c)
                            .findFirst()
                            .map(e -> switch (e.getKey()) {
                                case Item.Gold g -> "\uD83D\uDCB0";   // 💰
                                case Item.Health h -> "\uD83D\uDC8A"; // 💊
                                case Item.Exit ex -> "\uD83D\uDEAA";  // 🚪
                            })
                            .orElse(null);
                }

                // 3. Jeśli puste, sprawdź skałę
                if (symbol == null) {
                    symbol = cave.rock(row, column) ? "\uD83E\uDEA8" : "&nbsp;"; // 🪨 lub twarda spacja HTML
                }

                sb.append(String.format(cellTemplate, symbol));
            }
            sb.append("<br>"); // Nowa linia HTML
        }
        return sb.toString();
    }


    public void add(Item entity, Supplier<Location> generateLocation) {
        for (; ; ) {
            final var location = generateLocation.get();

            if (itemLocation.containsValue(location)) {
                continue;
            }

            if (playerLocation.containsValue(location)) {
                continue;
            }

            itemLocation.put(entity, location);

            return;
        }
    }

    public void add(Player entity, Supplier<Location> generateLocation) {
        for (; ; ) {
            final var location = generateLocation.get();

            if (itemLocation.containsValue(location)) {
                continue;
            }

            if (playerLocation.containsValue(location)) {
                continue;
            }

            playerMoves.put(entity, 0);

            playerLocation.put(entity, location);
            if (entity instanceof Player.HumanPlayer player) {
                playerHealth.put(player, 100);
                playerGold.put(player, 0);
            }

            return;
        }
    }

    public Location randomLocation() {
        for (; ; ) {
            final var row = rg.nextInt(cave.rows());
            final var column = rg.nextInt(cave.columns());
            if (cave.rock(row, column)) {
                continue;
            }

            return new Location(row, column);
        }
    }

    public void step(Collection<Action> commands) {
        if (commands == null) {
            // Logowanie błędu: "Received null command collection"
            return;
        }

        // Filtrowanie: usuwamy null-e, sprawdzamy poprawność Action i gracza
        final var filtered = commands.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.player() != null && a.direction() != null)
                .collect(Collectors.groupingBy(Action::player))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getFirst()
                ));
        // apply commands to player locations
        final var moved = playerLocation.entrySet().stream()
                .map(entry -> {
                    final var action = filtered.get(entry.getKey());
                    if (action == null) {
                        return entry;
                    }


                    if (entry.getKey() instanceof Player.HumanPlayer human) {
                        playerMoves.computeIfPresent(entry.getKey(), (k, v) -> v + 1);
                        playerHealth.computeIfPresent(human, (player, hp) -> Math.max(0, hp - 1));
                    }

                    final var next = move(entry.getValue(), action);

                    boolean hitRock = false;
                    if (next.row() < 0 || next.row() >= cave.rows() ||
                            next.column() < 0 || next.column() >= cave.columns()) {
                        hitRock = true;
                    } else if (cave.rock(next.row(), next.column())) {
                        hitRock = true;
                    }

                    if (hitRock) {
                        // Jeśli to człowiek, odejmij 5 HP
                        if (entry.getKey() instanceof Player.HumanPlayer human) {
                            playerHealth.computeIfPresent(human, (key, currentHp) -> Math.max(0, currentHp - 5));
                        }
                        return entry;
                    }

                    return Map.entry(entry.getKey(), next);
                })
                .collect(Collectors.groupingBy(Map.Entry::getValue));

        // fight and collect gems
        moved.forEach((key, value) -> fight(key, value.stream().map(Map.Entry::getKey).toList()));

        // update locations
        moved.forEach((location, entries) -> entries.forEach(entry -> playerLocation.put(entry.getKey(), entry.getValue())));

        // generate gold if none
        long goldCount = itemLocation.keySet().stream()
                .filter(item -> item instanceof Item.Gold)
                .count();

        if (goldCount < NUM_GOLD) {
            generateGold(NUM_GOLD - (int) goldCount);
        }


        long healthCount = itemLocation.keySet().stream()
                .filter(item -> item instanceof Item.Health)
                .count();

        if (healthCount < NUM_HEALTH) {
            generateHealth(NUM_HEALTH - (int) healthCount);
        }


    }

    private void generateHealth(int amount) {
        for (int i = 0; i < amount; i++) {
            add(new Item.Health(i, ThreadLocalRandom.current().nextInt(100)), this::randomLocation);
        }
    }


    private void generateGold(int amount) {
        for (int i = 0; i < amount; i++) {
            add(new Item.Gold(i, ThreadLocalRandom.current().nextInt(100)), this::randomLocation);
        }
    }


    private void fight(Location location, List<Player> players) {
        if (players.isEmpty()) return;

        List<Player.HumanPlayer> humans = players.stream()
                .filter(p -> p instanceof Player.HumanPlayer)
                .map(p -> (Player.HumanPlayer) p)
                .toList();

        List<Player.Dragon> dragons = players.stream()
                .filter(p -> p instanceof Player.Dragon)
                .map(p -> (Player.Dragon) p)
                .toList();

        // SMOCZY ODDECH
        for (Player.Dragon dragon : dragons) {
            int damage = switch (dragon.size()) {
                case Small -> 5;
                case Medium -> 15;
                case Large -> 40;
            };
            humans.forEach(h -> playerHealth.computeIfPresent(h, (k, hp) -> Math.max(0, hp - damage)));
        }

        // AWANTURA
        if (humans.size() > 1) {
            int pvpDamage = (humans.size() - 1) * 10;
            humans.forEach(h -> playerHealth.computeIfPresent(h, (k, hp) -> Math.max(0, hp - pvpDamage)));
        }

        // --- NOWA LOGIKA LOOTU ---

        // 1. Filtrujemy tylko żywych graczy i sortujemy ich deterministycznie
        // Sortowanie: 1. Najwyższe HP (malejąco), 2. Imię (rosnąco)
        List<Player.HumanPlayer> aliveHumans = humans.stream()
                .filter(h -> playerHealth.getOrDefault(h, 0) > 0)
                .sorted(Comparator.comparingInt((Player.HumanPlayer h) -> playerHealth.getOrDefault(h, 0)).reversed()
                        .thenComparing(Player.HumanPlayer::name))
                .toList();

        if (!aliveHumans.isEmpty()) {
            // Obliczamy sumaryczne HP wszystkich żywych na polu
            int totalHp = aliveHumans.stream()
                    .mapToInt(h -> playerHealth.getOrDefault(h, 0))
                    .sum();

            // Znajdujemy przedmioty na tej pozycji
            var itemsAtLocation = itemLocation.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(location))
                    .map(Map.Entry::getKey)
                    .toList();

            for (Item item : itemsAtLocation) {
                // Handle Exit - player finishes the game
                if (item instanceof Item.Exit) {
                    // Win logic: add 100 to health for all alive humans at exit
                    for (var human : aliveHumans) {
                        playerHealth.computeIfPresent(human, (k, v) -> Math.min(v + 100, 200)); // Allow exceeding normal max for winners
                        winnersWhoReachedExit.add(human);
                    }
                    
                    // Save results and mark game as ended
                    if (!winnersWhoReachedExit.isEmpty()) {
                        saveGameResults();
                        gameEnded = true;
                    }
                    continue;
                }
                
                int itemValue = switch (item) {
                    case Item.Gold g -> g.value();
                    case Item.Health h -> h.value();
                    case Item.Exit e -> 0;
                };

                int distributedTotal = 0;
                Map<Player.HumanPlayer, Integer> shares = new HashMap<>();

                // Podział proporcjonalny (floor)
                for (var h : aliveHumans) {
                    int hp = playerHealth.getOrDefault(h, 0);
                    int share = (int) Math.floor((double) itemValue * hp / totalHp);
                    shares.put(h, share);
                    distributedTotal += share;
                }

                // 3. Rozdzielenie reszty deterministycznie (dla pierwszego na liście po sortowaniu)
                int remainder = itemValue - distributedTotal;
                if (remainder > 0) {
                    Player.HumanPlayer luckyWinner = aliveHumans.get(0);
                    shares.put(luckyWinner, shares.get(luckyWinner) + remainder);
                }

                // 4. Aktualizacja map zdrowia i złota
                shares.forEach((h, amount) -> {
                    if (item instanceof Item.Gold) {
                        playerGold.computeIfPresent(h, (k, v) -> Math.min(v + amount, 100));
                    } else if (item instanceof Item.Health) {
                        playerHealth.computeIfPresent(h, (k, v) -> Math.min(v + amount, 100));
                    }
                });

                itemLocation.remove(item);
            }
        }
    }

    private Location move(Location value, Action action) {
        return switch (action.direction()) {
            case Up -> new Location(value.row() - 1, value.column());
            case Down -> new Location(value.row() + 1, value.column());
            case Left -> new Location(value.row(), value.column() - 1);
            case Right -> new Location(value.row(), value.column() + 1);
        };
    }

    public Integer health(Player.HumanPlayer player) {
        return playerHealth.get(player);
    }

    public Integer gold(Player.HumanPlayer player) {
        return playerGold.get(player);
    }
    
    public void generateExit() {
        add(new Item.Exit(0), this::randomLocation);
    }
    
    public Location getExitLocation() {
        return itemLocation.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof Item.Exit)
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
    
    public Map<Location, Integer> calculateDistancesFromExit() {
        Location exitLocation = getExitLocation();
        if (exitLocation == null) {
            return new HashMap<>();
        }
        
        return bfsDistances(exitLocation);
    }
    
    private Map<Location, Integer> bfsDistances(Location start) {
        Map<Location, Integer> distances = new HashMap<>();
        Queue<Location> queue = new LinkedList<>();
        
        queue.add(start);
        distances.put(start, 0);
        
        while (!queue.isEmpty()) {
            Location current = queue.poll();
            int currentDistance = distances.get(current);
            
            for (Direction dir : Direction.values()) {
                Location next = moveLocation(current, dir);
                
                // Check if valid and not visited
                if (isValidLocation(next) && !distances.containsKey(next)) {
                    distances.put(next, currentDistance + 1);
                    queue.add(next);
                }
            }
        }
        
        return distances;
    }
    
    private Location moveLocation(Location location, Direction direction) {
        return switch (direction) {
            case Up -> new Location(location.row() - 1, location.column());
            case Down -> new Location(location.row() + 1, location.column());
            case Left -> new Location(location.row(), location.column() - 1);
            case Right -> new Location(location.row(), location.column() + 1);
        };
    }
    
    private boolean isValidLocation(Location location) {
        if (location.row() < 0 || location.row() >= cave.rows() ||
            location.column() < 0 || location.column() >= cave.columns()) {
            return false;
        }
        return !cave.rock(location.row(), location.column());
    }
    
    public List<Location> getLocationsAtDistance(int distance) {
        Map<Location, Integer> distances = calculateDistancesFromExit();
        return distances.entrySet().stream()
                .filter(entry -> entry.getValue() == distance)
                .map(Map.Entry::getKey)
                .filter(loc -> !itemLocation.containsValue(loc))
                .filter(loc -> !playerLocation.containsValue(loc))
                .toList();
    }
    
    public Supplier<Location> fairStartLocationSupplier(int targetDistance) {
        return () -> {
            List<Location> candidates = getLocationsAtDistance(targetDistance);
            if (candidates.isEmpty()) {
                // Fallback to random if no locations at exact distance
                return randomLocation();
            }
            return candidates.get(rg.nextInt(candidates.size()));
        };
    }
    
    /**
     * Save game results to a file when a player reaches the exit
     */
    private void saveGameResults() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "game_results_" + timestamp + ".txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=== GAME RESULTS ===");
            writer.println("Timestamp: " + LocalDateTime.now());
            writer.println();
            
            writer.println("=== WINNERS (Reached Exit) ===");
            for (Player.HumanPlayer winner : winnersWhoReachedExit) {
                int health = playerHealth.getOrDefault(winner, 0);
                int gold = playerGold.getOrDefault(winner, 0);
                int moves = playerMoves.getOrDefault(winner, 0);
                int score = calculateScore(winner);
                
                writer.println("Player: " + winner.name());
                writer.println("  Health: " + health);
                writer.println("  Gold: " + gold);
                writer.println("  Moves: " + moves);
                writer.println("  Score: " + score);
                writer.println();
            }
            
            writer.println("=== ALL PLAYERS ===");
            for (Map.Entry<Player, Location> entry : playerLocation.entrySet()) {
                if (entry.getKey() instanceof Player.HumanPlayer human) {
                    int health = playerHealth.getOrDefault(human, 0);
                    int gold = playerGold.getOrDefault(human, 0);
                    int moves = playerMoves.getOrDefault(human, 0);
                    int score = calculateScore(human);
                    boolean reachedExit = winnersWhoReachedExit.contains(human);
                    
                    writer.println("Player: " + human.name());
                    writer.println("  Health: " + health);
                    writer.println("  Gold: " + gold);
                    writer.println("  Moves: " + moves);
                    writer.println("  Score: " + score);
                    writer.println("  Reached Exit: " + reachedExit);
                    writer.println();
                }
            }
            
            System.out.println("Game results saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to save game results: " + e.getMessage());
        }
    }
    
    /**
     * Calculate score for a player: gold + health bonus - time penalty
     */
    private int calculateScore(Player.HumanPlayer player) {
        int gold = playerGold.getOrDefault(player, 0);
        int health = playerHealth.getOrDefault(player, 0);
        int moves = playerMoves.getOrDefault(player, 0);
        
        // Score formula: gold + health - (moves / 10)
        return gold + health - (moves / 10);
    }
    
    /**
     * Check if the game has ended (at least one player reached the exit)
     */
    public boolean isGameEnded() {
        return gameEnded;
    }
}
