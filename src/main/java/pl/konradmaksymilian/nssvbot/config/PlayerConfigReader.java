package pl.konradmaksymilian.nssvbot.config;

import pl.konradmaksymilian.nssvbot.management.Player;

import java.util.Optional;

public class PlayerConfigReader extends PropertiesConfigReader<PlayerConfig> {

    public static final String FILE = "players.properties";

    @Override
    public Optional<PlayerConfig> read() {
        var players = readFile(FILE);
        if (players.isEmpty()) {
            return Optional.empty();
        }

        var configBuilder = PlayerConfig.builder();

        players.get().forEach((nick, data) -> {
            String[] playerData = ((String) data).split(", ");
            if (playerData.length == 1) {
                configBuilder.add(new Player((String) nick, playerData[0], null));
            } else if (playerData.length == 2) {
                configBuilder.add(new Player((String) nick, playerData[0], playerData[1]));
            } else {
                throw new ConfigException("Data is not valid: player with nick: '" + nick + "' cannot be read");
            }
        });
        return Optional.of(configBuilder.build());
    }
}
