package pl.konradmaksymilian.nssvbot.config;

import java.io.ObjectInputFilter;
import java.util.Optional;

public class DealerConfigReader extends PropertiesConfigReader<DealerConfig> {

    public static final String FILE = "dealer.properties";

    @Override
    public Optional<DealerConfig> read() {
        var properties = readFile(FILE);
        if (properties.isEmpty()) {
            return Optional.empty();
        }
        var data = properties.get();

        return Optional.of(new DealerConfig((String) data.get("tpToPlot"), (String) data.get("tpToShop")));
    }
}
