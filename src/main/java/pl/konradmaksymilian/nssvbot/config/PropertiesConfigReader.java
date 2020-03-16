package pl.konradmaksymilian.nssvbot.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public abstract class PropertiesConfigReader<T> implements ConfigReader<T> {

    protected Optional<Properties> readFile(String file) {
        var input = getClass().getClassLoader().getResourceAsStream(file);
        if (input == null) {
            return Optional.empty();
        }

        var config = new Properties();
        try {
            config.load(input);
        } catch (FileNotFoundException e) {
            config = null;
        } catch (IOException e) {
            throw new ConfigException("Cannot read data from file '" + file + "'");
        }
        return Optional.ofNullable(config);
    }
}
