package example.renderer;

import example.domain.Response;
import example.domain.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class CaveRenderer {
    private static final Logger logger = LoggerFactory.getLogger(CaveRenderer.class);
    
    private static final String ROCK = "█";
    private static final String EMPTY = " ";
    private static final String HUMAN_PLAYER = "P";
    private static final String DRAGON = "D";
    private static final String GOLD = "G";
    private static final String HEALTH = "H";
    private static final String EXIT = "E";
    private static final String BORDER = "═";
    private static final String CORNER_TL = "╔";
    private static final String CORNER_TR = "╗";
    private static final String CORNER_BL = "╚";
    private static final String CORNER_BR = "╝";
    private static final String VERTICAL = "║";
    
    public void renderCave(Cave cave, 
                          Collection<Response.StateLocations.ItemLocation> itemLocations,
                          Collection<Response.StateLocations.PlayerLocation> playerLocations,
                          String playerName,
                          Integer health,
                          Integer gold) {
        if (cave == null) {
            logger.warn("Cave is null, cannot render");
            return;
        }
        
        // Clear console (works on most terminals)
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        // Create a 2D grid to represent the cave
        String[][] grid = new String[cave.rows()][cave.columns()];
        
        // Initialize with rocks and empty spaces
        for (int row = 0; row < cave.rows(); row++) {
            for (int col = 0; col < cave.columns(); col++) {
                grid[row][col] = cave.rock(row, col) ? ROCK : EMPTY;
            }
        }
        
        // Place items
        if (itemLocations != null) {
            for (Response.StateLocations.ItemLocation itemLoc : itemLocations) {
                int row = itemLoc.location().row();
                int col = itemLoc.location().column();
                if (isValidPosition(row, col, cave)) {
                    if (itemLoc.entity() instanceof Item.Gold) {
                        grid[row][col] = GOLD;
                    } else if (itemLoc.entity() instanceof Item.Health) {
                        grid[row][col] = HEALTH;
                    } else if (itemLoc.entity() instanceof Item.Exit) {
                        grid[row][col] = EXIT;
                    }
                }
            }
        }
        
        // Place players
        if (playerLocations != null) {
            for (Response.StateLocations.PlayerLocation playerLoc : playerLocations) {
                int row = playerLoc.location().row();
                int col = playerLoc.location().column();
                if (isValidPosition(row, col, cave)) {
                    if (playerLoc.entity() instanceof Player.HumanPlayer) {
                        grid[row][col] = HUMAN_PLAYER;
                    } else if (playerLoc.entity() instanceof Player.Dragon) {
                        grid[row][col] = DRAGON;
                    }
                }
            }
        }
        
        // Render the grid
        renderHeader(playerName, health, gold);
        renderGrid(grid);
        renderLegend();
    }
    
    private void renderHeader(String playerName, Integer health, Integer gold) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.printf("║ Player: %-20s Health: %-5s Gold: %-5s ║%n", 
                         playerName != null ? playerName : "Unknown",
                         health != null ? health : "?",
                         gold != null ? gold : "?");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
    }
    
    private void renderGrid(String[][] grid) {
        // Top border
        System.out.print(CORNER_TL);
        for (int col = 0; col < grid[0].length; col++) {
            System.out.print(BORDER);
        }
        System.out.println(CORNER_TR);
        
        // Grid content
        for (int row = 0; row < grid.length; row++) {
            System.out.print(VERTICAL);
            for (int col = 0; col < grid[row].length; col++) {
                System.out.print(grid[row][col]);
            }
            System.out.println(VERTICAL);
        }
        
        // Bottom border
        System.out.print(CORNER_BL);
        for (int col = 0; col < grid[0].length; col++) {
            System.out.print(BORDER);
        }
        System.out.println(CORNER_BR);
    }
    
    private void renderLegend() {
        System.out.println("\nLegend:");
        System.out.println("  " + ROCK + " = Rock");
        System.out.println("  " + HUMAN_PLAYER + " = Human Player");
        System.out.println("  " + DRAGON + " = Dragon");
        System.out.println("  " + GOLD + " = Gold");
        System.out.println("  " + HEALTH + " = Health");
        System.out.println("  " + EXIT + " = Exit");
        System.out.println("  " + EMPTY + " = Empty Space");
    }
    
    private boolean isValidPosition(int row, int col, Cave cave) {
        return row >= 0 && row < cave.rows() && col >= 0 && col < cave.columns();
    }
}
