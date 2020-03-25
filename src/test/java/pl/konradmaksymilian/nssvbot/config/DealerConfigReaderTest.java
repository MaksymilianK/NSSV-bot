package pl.konradmaksymilian.nssvbot.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DealerConfigReaderTest {

    private DealerConfigReader dealerConfigReader;

    @BeforeEach
    public void setUp() {
        this.dealerConfigReader = new DealerConfigReader();
    }

    @Test
    public void readConfig() {
        var config = dealerConfigReader.read();

        assertThat(config).isPresent();
        var dealerConfig = config.get();

        assertThat(dealerConfig.getTpToPlot()).isEqualTo("/p home mikolajsoldier");
        assertThat(dealerConfig.getTpToShop()).isEqualTo("/home");
    }
}
