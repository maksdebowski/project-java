package example.game;

import example.domain.game.*;

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
    private final Cave cave;

    public Map<Player, Integer> playerHealth() {
        return Collections.unmodifiableMap(playerHealth);
    }

    public Map<Player, Integer> playerGold() {
        return Collections.unmodifiableMap(playerGold);
    }

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

                    final var next = move(entry.getValue(), action);

                    if (next.row() < 0 || next.row() >= cave.rows() ||
                            next.column() < 0 || next.column() >= cave.columns()) {
                        return entry;
                    }


                    if (cave.rock(next.row(), next.column())) {
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
        if (players.isEmpty()) {
            return;
        }

        // "fight" - health of all players is reduced by half of health of the weakest one
        if (players.size() > 1) {
            final var optionalMinimum = players.stream().min((o1, o2) -> Integer.compare(playerHealth.get(o1), playerHealth.get(o2))).map(playerHealth::get);

            optionalMinimum.ifPresent(minimum -> players.forEach(player -> playerHealth.compute(player, (ignored, health) -> (2 * health - minimum + 1) / 2)));
        }

        final var optionalMaximum = players.stream().max((o1, o2) -> Integer.compare(playerHealth.get(o1), playerHealth.get(o2)));

        optionalMaximum.ifPresent(maximum -> {
            final var filtered = itemLocation.entrySet().stream().filter(entry -> entry.getValue().equals(location)).toList();

            // do something with non player entities, i.e. gold?
            itemLocation.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(location))
                    .forEach(entry -> {
                        switch (entry.getKey()) {
                            case Item.Gold(int id, int value) -> {
                                playerGold.computeIfPresent(maximum, (ignored, current) -> current + value);
                            }

                            case Item.Health(int id, int value) -> {
                                playerHealth.computeIfPresent(maximum, (ignored, current) -> current + value);
                            }
                        }
                    });

            filtered.forEach(entry -> itemLocation.remove(entry.getKey()));
        });
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
}
