package pl.konradmaksymilian.nssvbot.config;

import java.util.Optional;

public interface ConfigReader<T> {

    public Optional<T> read();
}
