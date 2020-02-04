package pl.konradmaksymilian.nssvbot.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.management.command.AttachCommand;
import pl.konradmaksymilian.nssvbot.management.command.JoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.active.AdCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.active.LeaveCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.management.command.detached.LeaveCommandDetached;

public class SessionsManagerTest {

    private SessionsManager sessionsManager;
    
    private Properties config;

    @Mock
    private SessionFactory sessionFactory;
    
    @Mock
    private Consumer<Object> onMessage;
    
    @Mock
    private Session session;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(sessionFactory.create()).thenReturn(session);
        config = new Properties();
        config.put("player1", "pass1, p1");
        config.put("player2", "pass2");
        config.put("player3", "pass3, p3");
        
        sessionsManager = new SessionsManager(sessionFactory);
        sessionsManager.onMessage(this.onMessage);
        sessionsManager.setPlayers(config);
    }
    
    @Test
    public void initializingPlayersFromProperties() {
        var players = sessionsManager.getPlayers();
        assertThat(players).hasSize(3);
        assertThat(sessionsManager.getPlayers()).anyMatch(player -> {
            if (player.getAlias().isEmpty()) {
                return false;
            } else {
                return player.getNick().equals("player1") && player.getPassword().equals("pass1") 
                        && player.getAlias().get().equals("p1");
            }
        });
        assertThat(sessionsManager.getPlayers()).anyMatch(player -> player.getNick().equals("player2") 
                && player.getPassword().equals("pass2") && player.getAlias().isEmpty());
        assertThat(sessionsManager.getPlayers()).anyMatch(player -> {
            if (player.getAlias().isEmpty()) {
                return false;
            } else {
                return player.getNick().equals("player3") && player.getPassword().equals("pass3") 
                        && player.getAlias().get().equals("p3");
            }
        });
    }
    
    @Test
    public void initializing() {
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
        assertThat(sessionsManager.waitsForPassword()).isFalse();
    }
    
    @Test
    public void waitsForPasswordAfterJoinCommandWithUnknownPlayer() {
        sessionsManager.join(new JoinCommand("player1000"));
        
        assertThat(sessionsManager.waitsForPassword()).isTrue();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void returnFalseAfterJoinCommandWithUnknownPlayer() {
        boolean joined = sessionsManager.join(new JoinCommand("player1000"));
        
        assertThat(joined).isFalse();
    }
    
    @Test
    public void createNewSessionAfterJoinCommandWithKnownNick() {
        sessionsManager.join(new JoinCommand("player1"));
       
        assertThat(sessionsManager.waitsForPassword()).isFalse();
        assertThat(sessionsManager.getStatus()).hasSize(1);
    }
    
    @Test
    public void returnTrueAfterJoinCommandWithKnownNick() {
        boolean joined = sessionsManager.join(new JoinCommand("player1"));
        
        assertThat(joined).isTrue();
    }
    
    @Test
    public void createNewSessionAfterJoinCommandWithKnownAlias() {
        sessionsManager.join(new JoinCommand("p1"));
       
        assertThat(sessionsManager.waitsForPassword()).isFalse();
        assertThat(sessionsManager.getStatus()).hasSize(1);
        verify(session).joinServer(argThat(player -> {
            if (player.getAlias().isEmpty()) {
                return false;
            } else {
                return player.getNick().equals("player1") && player.getPassword().equals("pass1") 
                        && player.getAlias().get().equals("p1");
            }
        }), any());
    }
    
    @Test
    public void createNewSessionAfterPasswordWhenWaitsForPassword() {
        sessionsManager.join(new JoinCommand("player1000"));
        
        sessionsManager.join("password");
        
        assertThat(sessionsManager.waitsForPassword()).isFalse();
        assertThat(sessionsManager.getStatus()).hasSize(1);
    }
    
    @Test
    public void throwExceptionAfterPasswordWhenNotWaitingForIt() {        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.join("password"))
                .withMessage("Cannot provide password while not waiting for it");
    }
    
    @Test
    public void throwExceptionOnJoinCommandWhenWaitsForPassword() {
        sessionsManager.join(new JoinCommand("player1000"));

        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() ->  sessionsManager.join(new JoinCommand("player2000")))
                .withMessage("Cannot join when waiting for a password");
    }
    
    @Test
    public void attachIfSessionsExistsAndExistingNick() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThat(sessionsManager.isAnyActive()).isTrue();
        verify(session).setActive(true);
    }
    
    @Test
    public void throwExceptionIfAttachToSessionAlreadyAttachedTo() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.attach(new AttachCommand("player1")))
                .withMessage("Cannot attach to a session while already attached to it");
        
    }
    
    @Test
    public void throwExceptionIfAttachToNonExistingSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        
        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.attach(new AttachCommand("player1")))
                .withMessage("Cannot attach to the session because it does not exist");
    }
    
    @Test
    public void detachIfAttachedToSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.detach();
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        verify(session).setActive(false);
    }
    
    @Test
    public void returnTrueAfterDetached() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThat(sessionsManager.detach()).isTrue();
    }
    
    @Test
    public void returnFalseAfterDetachFail() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        assertThat(sessionsManager.detach()).isFalse();
    }
    
    @Test
    public void setAdActiveIfSessionActive() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.setAdActive(new AdCommandActive(65, "ad"));
        
        verify(session).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void throwExceptionIfSettingAdActiveIfNoActiveSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.setAdActive(new AdCommandActive(65, "ad")))
                .withMessage("There is no active session");
    }
    
    @Test
    public void setAdDetachedIfNoActiveSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad"));
        
        verify(session).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void setAdDetachedWithAlias() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.setAdDetached(new AdCommandDetached("p1", 65, "ad"));
        
        verify(session).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void setAdDetachedIfSessionActive() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad"));
        
        verify(session).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void throwExceptionIfSettingAdDetachedIfNoSuchSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        
        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad")))
                .withMessage("Cannot find a player - player1");
    }
    
    @Test
    public void leaveOnLeaveActiveIfSessionActive() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.leaveActive();
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void throwExceptionOnLeaveActiveIfNoActiveSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.leaveActive())
                .withMessage("There is no active session - nick or alias not provided");
    }
    
    @Test
    public void leaveOnLeaveDetachedIfNoActiveSession() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.leaveDetached(new LeaveCommandDetached("player1"));
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void leaveOnLeaveDetachedWithAlias() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.leaveDetached(new LeaveCommandDetached("p1"));
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void throwExceptionOnLeaveDetachedIfSessionActive() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.leaveDetached(new LeaveCommandDetached("player2")))
                .withMessage("Cannot leave when attached to a session");
    }
    
    @Test
    public void sendMessageIfSessionActive() {
        when(session.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(session.getStatus()).thenReturn(Status.GAME);
        when(session.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.sendMessage("message");
        
        verify(session).sendChatMessage("message");
    }
    
    @Test
    public void throwExceptionWhenSendMessageIfNoActiveSession() {        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.sendMessage("message"))
                .withMessage("There is no active sessions to send a message");
    }
}
