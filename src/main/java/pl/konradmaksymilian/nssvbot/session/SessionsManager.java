package pl.konradmaksymilian.nssvbot.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.management.ConfigException;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.management.command.AttachCommand;
import pl.konradmaksymilian.nssvbot.management.command.JoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.active.AdCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.active.LeaveCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.management.command.detached.LeaveCommandDetached;

public class SessionsManager {

    private final Set<Session> sessions = new HashSet<>();
    private final Map<String, Thread> threads = new HashMap<>();
    private final Set<Player> players = new HashSet<>();
    private final SessionFactory sessionFactory;
    
    private Consumer<Object> onMessage;
    private JoinCommand waitingJoinCommand = null;
    private Session active = null;
    
    public SessionsManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public void setPlayers(Properties players) {
        players.forEach((nick, data) -> {
            String[] playerData = ((String) data).split(", ");
            if (playerData.length == 1) {
                addPlayer((String) nick, playerData[0], null);
            } else if (playerData.length == 2) {
                addPlayer((String) nick, playerData[0], playerData[1]);
            } else {
                throw new ConfigException("Data is not valid; player with nick: '" + nick + "' cannot be initialized");
            }
        });
    }
    
    public void onMessage(Consumer<Object> onMessage) {
        this.onMessage = onMessage;
    }
    
    public boolean waitsForPassword() {
        return waitingJoinCommand != null;
    }
    
    public void sendMessage(String message) {
        if (!isAnyActive()) {
            throw new IllegalMethodInvocationException("There is no active sessions to send a message");
        }
        
        active.sendChatMessage(message);
    }
    
    public boolean isAnyActive() {
        return active != null;
    }
    
    public boolean join(JoinCommand command) {
        if (active != null) {
            throw new SessionException("Cannot join when attached to a session");
        } else if (waitsForPassword()) {
            throw new IllegalMethodInvocationException("Cannot join when waiting for a password");
        }
        
        var playerData = players.stream()
                .filter(player -> matchesNickOrAlias(player, command.getNickOrAlias()))
                .findAny();
        
        if (playerData.isPresent()) {
            startNewSession(playerData.get());
            return true;
        } else {
            waitingJoinCommand = command;
            return false;
        }
    }
    
    public void join(String password) {
        if (!waitsForPassword()) {
            throw new IllegalMethodInvocationException("Cannot provide password while not waiting for it");
        } else {
            startNewSession(new Player(waitingJoinCommand.getNickOrAlias(), password, null));
            waitingJoinCommand = null;
        }
    }

    public void attach(AttachCommand command) {
        var optionalSession = findSession(command.getNickOrAlias());
        
        if (optionalSession.isPresent()) {
            if (active == optionalSession.get()) {
                throw new SessionException("Cannot attach to a session while already attached to it");
            } else {
                setActive(optionalSession.get());
            }
        } else {
            throw new SessionException("Cannot attach to the session because it does not exist");
        }
    }
    
    public boolean detach() {
        if (active == null) {
            return false;
        } else {
            doDetach();
            return true;
        }        
    }
    
    public boolean setAdActive(AdCommandActive command) {
        if (active == null) {
            throw new IllegalMethodInvocationException("There is no active session");
        }
        
        return setAd(command.getDuration(), command.getText(), active);
    }
    
    public boolean setAdDetached(AdCommandDetached command) {
        return setAd(command.getDuration(), command.getText(), findSession(command.getNickOrAlias()).orElseThrow(
                () -> new SessionException("Cannot find a player - " + command.getNickOrAlias())));
    }
    
    public void leaveActive() {
        if (!isAnyActive()) {
            throw new IllegalMethodInvocationException("There is no active session - nick or alias not provided");
        }
            
        stopAndRemoveSession(active);
    }
    
    public void leaveDetached(LeaveCommandDetached command) {
        if (isAnyActive()) {
            throw new IllegalMethodInvocationException("Cannot leave when attached to a session");
        }
        
        stopAndRemoveSession(findSession(command.getNickOrAlias()).orElseThrow(() -> new SessionException(
                "Cannot find a player - " + command.getNickOrAlias())));
    }
    
    public Map<Player, Status> getStatus() {
        var status = new HashMap<Player, Status>();
        sessions.forEach(session -> status.put(session.getPlayer(), session.getStatus()));
        return status;
    }
    
    public Set<Player> getPlayers() {
        return Set.copyOf(players);
    }
        
    private boolean setAd(int duration, String text, Session session) {
        if (duration < 0) {
            session.setAd(new Advert(duration, text));
            return false;
        } else if (duration < 60) {
            throw new SessionException("Duration is too short; cannot set the advert");
        } else {
            session.setAd(new Advert(duration, text));
            return true;
        }
    }
    
    private void setActive(Session session) {
        if (active != null) {            
            active.setActive(false);
        }
        active = session;
        session.setActive(true);
    }
    
    private void addPlayer(String nick, String password, String alias) {
        players.add(new Player(nick, password, alias));
    }
    
    private void startNewSession(Player player) {
        if (sessions.stream().anyMatch(session -> session.getPlayer().equals(player))) {
            throw new SessionException("The player is already in the server");
        }
        
        var session = sessionFactory.create();
        var thread = new Thread(() -> session.joinServer(player, message -> onMessage.accept(message)));
        thread.start();
        sessions.add(session);
        threads.put(player.getNick(), thread);
    }
    
    private Optional<Session> findSession(String nickOrAlias) {
        return sessions.stream()
                .filter(session -> matchesNickOrAlias(session.getPlayer(), nickOrAlias))
                .findAny();
    }
    
    private boolean matchesNickOrAlias(Player player, String nickOrAlias) {
        if (player.getNick().equals(nickOrAlias)) {
            return true;
        } else if (player.getAlias().isPresent()) {
            return player.getAlias().get().equals(nickOrAlias);
        } else {
            return false;
        }
    }
    
    private void stopAndRemoveSession(Session session) {
        if (active == session) {
            doDetach();
        }
        sessions.remove(session);
        threads.get(session.getPlayer().getNick()).interrupt();
        threads.remove(session.getPlayer().getNick());
    }
    
    private void doDetach() {
        active.setActive(false);
        active = null;
    }
}
