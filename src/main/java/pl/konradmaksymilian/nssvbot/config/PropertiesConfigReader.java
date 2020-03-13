package pl.konradmaksymilian.nssvbot.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

public abstract class PropertiesConfigReader<T> implements ConfigReader<T> {

    protected Optional<Properties> readFile(String file) {
        URL url = getClass().getClassLoader().getResource(file);
        if (url == null) {
            return Optional.empty();
        }

        var config = new Properties();
        try {
            config.load(new FileInputStream(url.getPath()));
        } catch (FileNotFoundException e) {
            config = null;
        } catch (IOException e) {
            throw new ConfigException("Cannot read data from file '" + url.getPath() + "'");
        }
        return Optional.ofNullable(config);
    }
}
