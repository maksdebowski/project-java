package example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GameConfig {
    private static final Logger logger = LoggerFactory.getLogger(GameConfig.class);
    private static final String CONFIG_FILE = "config.properties";
    
    private final Properties properties;
    
    public GameConfig() {
        this.properties = new Properties();
        loadConfig();
    }
    
    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                logger.error("Unable to find {}", CONFIG_FILE);
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(input);
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    public String getServerHost() {
        return properties.getProperty("server.host", "localhost");
    }
    
    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }
    
    public String getPlayerName() {
        return properties.getProperty("player.name", "Player");
    }
    
    public String getPlayerKey() {
        return properties.getProperty("player.key", "");
    }
}
