package pl.konradmaksymilian.nssvbot.management;

import java.util.Optional;

public final class Player {
    
    private final String nick;
    private final String password;
    private final String alias;
    
    public Player(String nick, String password, String alias) {
        this.nick = nick;
        this.password = password;
        this.alias = alias;
    }
    
    public String getNick() {
        return nick;
    }
    
    public String getPassword() {
        return password;
    }
    
    public Optional<String> getAlias() {
        return Optional.ofNullable(alias);
    }
    
    @Override
    public boolean equals(Object player2) {
        if (player2 != null) {
            if (player2 instanceof Player) {
                return nick.equals(((Player) player2).getNick());
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return 17 * nick.hashCode();
    }
}
