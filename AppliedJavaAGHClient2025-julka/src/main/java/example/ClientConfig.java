package example;

public class ClientConfig {
    public static String getKey() {
        return System.getProperty("Julka", "Haslo_Julki");
    }
}
