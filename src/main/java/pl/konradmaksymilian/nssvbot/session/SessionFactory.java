package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;

public class SessionFactory {

    public Session create() {
        return new Session(new ConnectionManager(), new Timer());
    }
}
