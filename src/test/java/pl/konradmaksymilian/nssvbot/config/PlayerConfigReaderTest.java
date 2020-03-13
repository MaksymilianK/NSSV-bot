package pl.konradmaksymilian.nssvbot.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.konradmaksymilian.nssvbot.management.Player;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayerConfigReaderTest {

    private PlayerConfigReader playerConfigReader;

    @BeforeEach
    public void setUp() {
        this.playerConfigReader = new PlayerConfigReader();
    }

    @Test
    public void readPlayers() {
        var config = playerConfigReader.read();

        assertThat(config).isPresent();
        var players = config.get().getPlayers();

        assertThat(players).hasSize(4);

        assertThat(players).anyMatch(player -> {
            if (player.getAlias().isPresent()) {
                return player.getNick().equals("nick1") && player.getPassword().equals("password1")
                        && player.getAlias().get().equals("alias1");
            } else {
                return false;
            }
        });

        assertThat(players).anyMatch(player -> player.getNick().equals("nick2") && player.getPassword().equals("password2")
                && player.getAlias().isEmpty());

        assertThat(players).anyMatch(player -> {
            if (player.getAlias().isPresent()) {
                return player.getNick().equals("nick3") && player.getPassword().equals("password3")
                        && player.getAlias().get().equals("alias3");
            } else {
                return false;
            }
        });

        assertThat(players).anyMatch(player -> player.getNick().equals("nick4") && player.getPassword().equals("password4")
                && player.getAlias().isEmpty());
    }
}
