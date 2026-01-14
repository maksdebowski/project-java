package example.domain.configuration;

import java.util.Collection;

public record Config(Collection<PlayerConfiguration> known) {
}