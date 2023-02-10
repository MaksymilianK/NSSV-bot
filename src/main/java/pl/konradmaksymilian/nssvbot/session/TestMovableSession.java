package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.util.List;

public class TestMovableSession extends MovableSession {

    private static List<Double> X_DESTINATIONS = List.of(64.0, 64.0, 82.0, 82.0);
    private static List<Double> Z_DESTINATIONS = List.of(-738.0, -720.0, -738.0, -720.0);

    private int current_move = -2;

    public TestMovableSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
    }
}
