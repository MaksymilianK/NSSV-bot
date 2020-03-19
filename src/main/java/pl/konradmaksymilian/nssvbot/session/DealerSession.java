package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.config.DealerConfig;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;

import pl.konradmaksymilian.nssvbot.utils.Timer;

public class DealerSession extends MovableSession {

    private final DealerConfig config;

    public DealerSession(ConnectionManager connection, Timer timer, DealerConfig config) {
        super(connection, timer);
        this.config = config;
    }
}
