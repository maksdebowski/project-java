package example;

import example.domain.Response;

public class ConsoleRenderer {
    public static void render(LocalCave cave, GameState state) {
        if (cave == null || state == null) return;

        for (int r = 0; r < cave.rows(); r++) {
            for (int c = 0; c < cave.columns(); c++) {

                // skały
                if (cave.rock(r, c)) {
                    System.out.print("#");
                    continue;
                }

                // gracze
                boolean printed = false;
                if (state.playerLocations != null) {
                    for (Response.StateLocations.PlayerLocation p : state.playerLocations) {
                        if (p.location().row() == r && p.location().column() == c) {
                            System.out.print("P");
                            printed = true;
                            break;
                        }
                    }
                }
                if (printed) continue;

                // itemy
                if (state.itemLocations != null) {
                    for (Response.StateLocations.ItemLocation i : state.itemLocations) {
                        if (i.location().row() == r && i.location().column() == c) {
                            System.out.print("G"); // gold / health
                            printed = true;
                            break;
                        }
                    }
                }
                if (printed) continue;

                // puste pole
                System.out.print(".");
            }
            System.out.println();
        }

        System.out.println("Health: " + state.health + " | Gold: " + state.gold);
        System.out.println("=================================");
    }
}
