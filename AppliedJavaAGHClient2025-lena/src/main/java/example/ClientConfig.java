package example;

public class ClientConfig {
    public static String getKey() {
        return System.getProperty("player.key", "1234");
    }

}