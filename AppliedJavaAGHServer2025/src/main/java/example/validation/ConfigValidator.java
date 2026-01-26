package example.validation;

import example.domain.configuration.Config;
import example.domain.configuration.PlayerConfiguration;
import example.domain.game.Player;

import java.util.HashSet;
import java.util.Set;

public class ConfigValidator {
    private static final int MIN_KEY_LENGTH = 5;
    private static final int MAX_KEY_LENGTH = 50;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    public static ValidationResult validate(Config config) {
        if (config == null || config.known() == null) {
            return ValidationResult.failure("Configuration is null or has no known players");
        }
        
        if (config.known().isEmpty()) {
            return ValidationResult.failure("Configuration must contain at least one player");
        }
        
        Set<String> names = new HashSet<>();
        Set<String> keys = new HashSet<>();
        
        for (PlayerConfiguration playerConfig : config.known()) {
            // Validate player configuration is not null
            if (playerConfig == null) {
                return ValidationResult.failure("Player configuration cannot be null");
            }
            
            // Validate authorize
            if (playerConfig.authorize() == null) {
                return ValidationResult.failure("Authorization cannot be null");
            }
            
            // Validate key
            String key = playerConfig.authorize().key();
            if (key == null || key.trim().isEmpty()) {
                return ValidationResult.failure("Authorization key cannot be null or empty");
            }
            
            if (key.length() < MIN_KEY_LENGTH) {
                return ValidationResult.failure(
                    String.format("Authorization key '%s' is too short (minimum %d characters)", 
                                key, MIN_KEY_LENGTH));
            }
            
            if (key.length() > MAX_KEY_LENGTH) {
                return ValidationResult.failure(
                    String.format("Authorization key is too long (maximum %d characters)", 
                                MAX_KEY_LENGTH));
            }
            
            // Check for duplicate keys
            if (keys.contains(key)) {
                return ValidationResult.failure(
                    String.format("Duplicate authorization key found: '%s'", key));
            }
            keys.add(key);
            
            // Validate player
            if (playerConfig.player() == null) {
                return ValidationResult.failure("Player cannot be null");
            }
            
            // Validate player name (only for HumanPlayer)
            if (playerConfig.player() instanceof Player.HumanPlayer humanPlayer) {
                String name = humanPlayer.name();
                
                if (name == null || name.trim().isEmpty()) {
                    return ValidationResult.failure("Player name cannot be null or empty");
                }
                
                if (name.length() < MIN_NAME_LENGTH) {
                    return ValidationResult.failure(
                        String.format("Player name '%s' is too short (minimum %d characters)", 
                                    name, MIN_NAME_LENGTH));
                }
                
                if (name.length() > MAX_NAME_LENGTH) {
                    return ValidationResult.failure(
                        String.format("Player name '%s' is too long (maximum %d characters)", 
                                    name, MAX_NAME_LENGTH));
                }
                
                // Check for duplicate names
                if (names.contains(name)) {
                    return ValidationResult.failure(
                        String.format("Duplicate player name found: '%s'", name));
                }
                names.add(name);
            }
        }
        
        return ValidationResult.success();
    }
}
