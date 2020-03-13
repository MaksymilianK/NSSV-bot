package pl.konradmaksymilian.nssvbot.config;

import pl.konradmaksymilian.nssvbot.management.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PlayerConfig {

    private final Set<Player> players;

    private PlayerConfig(Set<Player> players) {
        this.players = players;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Set<Player> players = new HashSet<>();

        Builder() {}

        public Builder add(Player player) {
            players.add(player);
            return this;
        }

        public PlayerConfig build() {
            return new PlayerConfig(Collections.unmodifiableSet(players));
        }
    }
}
