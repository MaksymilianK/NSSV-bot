package pl.konradmaksymilian.nssvbot.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.config.PlayerConfig;
import pl.konradmaksymilian.nssvbot.config.PlayerConfigReader;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.management.command.AttachCommand;
import pl.konradmaksymilian.nssvbot.management.command.DealerJoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.JoinCommand;
import pl.konradmaksymilian.nssvbot.management.command.active.AdCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.active.LeaveCommandActive;
import pl.konradmaksymilian.nssvbot.management.command.detached.AdCommandDetached;
import pl.konradmaksymilian.nssvbot.management.command.detached.LeaveCommandDetached;

public class SessionsManagerTest {

    private SessionsManager sessionsManager;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private PlayerConfigReader playerConfigReader;
    
    @Mock
    private Consumer<Object> onMessage;
    
    @Mock
    private BasicAfkSession basicAfkSession;

    @Mock
    private DealerSession dealerSession;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(sessionFactory.createBasicAfk()).thenReturn(basicAfkSession);
        when(sessionFactory.createDealer()).thenReturn(dealerSession);

        PlayerConfig config = PlayerConfig.builder()
                .add(new Player("player1", "pass1", "p1"))
                .add(new Player("player2", "pass2", null))
                .add(new Player("player3", "pass3", "p3"))
                .build();
        when(playerConfigReader.read()).thenReturn(Optional.of(config));
        
        sessionsManager = new SessionsManager(sessionFactory, playerConfigReader);
        sessionsManager.onMessage(this.onMessage);
        sessionsManager.init();
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
    public void createNewBasicAfkSessionAfterJoinCommandWithKnownAlias() {
        sessionsManager.join(new JoinCommand("p1"));
       
        assertThat(sessionsManager.waitsForPassword()).isFalse();
        assertThat(sessionsManager.getStatus()).hasSize(1);
        verify(basicAfkSession).joinServer(argThat(player -> {
            if (player.getAlias().isEmpty()) {
                return false;
            } else {
                return player.getNick().equals("player1") && player.getPassword().equals("pass1") 
                        && player.getAlias().get().equals("p1");
            }
        }), any());
    }

    @Test
    public void createNewDealerSessionAfterJoinCommandWithKnownAlias() {
        sessionsManager.join(new DealerJoinCommand("p1"));

        assertThat(sessionsManager.waitsForPassword()).isFalse();
        assertThat(sessionsManager.getStatus()).hasSize(1);
        verify(dealerSession).joinServer(argThat(player -> {
            if (player.getAlias().isEmpty()) {
                return false;
            } else {
                return player.getNick().equals("player1") && player.getPassword().equals("pass1")
                        && player.getAlias().get().equals("p1");
            }
        }), any());
    }
    
    @Test
    public void createNewBasicAfkSessionAfterPasswordWhenWaitsForPassword() {
        sessionsManager.join(new JoinCommand("player1000"));
        
        sessionsManager.join("password");
        
        assertThat(sessionsManager.waitsForPassword()).isFalse();
        assertThat(sessionsManager.getStatus()).hasSize(1);
    }

    @Test
    public void createNewDealerSessionAfterPasswordWhenWaitsForPassword() {
        sessionsManager.join(new DealerJoinCommand("player1000"));

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
    public void throwExceptionOnJoinCommandWhenWaitsForPasswordForBasicAfkSession() {
        sessionsManager.join(new JoinCommand("player1000"));

        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() ->  sessionsManager.join(new JoinCommand("player2000")))
                .withMessage("Cannot join when waiting for a password");
    }

    @Test
    public void throwExceptionOnJoinCommandWhenWaitsForPasswordForDealerSession() {
        sessionsManager.join(new DealerJoinCommand("player1000"));

        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() ->  sessionsManager.join(new JoinCommand("player2000")))
                .withMessage("Cannot join when waiting for a password");
    }
    
    @Test
    public void attachIfSessionsExistsAndExistingNick() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThat(sessionsManager.isAnyActive()).isTrue();
        verify(basicAfkSession).setActive(true);
    }
    
    @Test
    public void throwExceptionIfAttachToSessionAlreadyAttachedTo() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.attach(new AttachCommand("player1")))
                .withMessage("Cannot attach to a session while already attached to it");
        
    }
    
    @Test
    public void throwExceptionIfAttachToNonExistingSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        
        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.attach(new AttachCommand("player1")))
                .withMessage("Cannot attach to the session because it does not exist");
    }
    
    @Test
    public void detachIfAttachedToSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.detach();
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        verify(basicAfkSession).setActive(false);
    }
    
    @Test
    public void returnTrueAfterDetached() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThat(sessionsManager.detach()).isTrue();
    }
    
    @Test
    public void returnFalseAfterDetachFail() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        assertThat(sessionsManager.detach()).isFalse();
    }
    
    @Test
    public void setAdActiveIfSessionActive() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.setAdActive(new AdCommandActive(65, "ad"));
        
        verify(basicAfkSession).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void throwExceptionIfSettingAdActiveIfNoActiveSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.setAdActive(new AdCommandActive(65, "ad")))
                .withMessage("There is no active session");
    }

    @Test
    public void throwExceptionOnSetAdAttachedIfSessionNotBasicAfkAndIsActive() {
        when(dealerSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(dealerSession.getStatus()).thenReturn(Status.GAME);
        when(dealerSession.isActive()).thenReturn(false);
        sessionsManager.join(new DealerJoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));

        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.setAdActive(new AdCommandActive(65, "ad")))
                .withMessage("The session is not able to advertise");
    }
    
    @Test
    public void setAdDetachedIfNoActiveSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad"));
        
        verify(basicAfkSession).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }

    @Test
    public void throwExceptionOnSetAdDetachedIfNotBasicAfkAndIsNotActive() {
        when(dealerSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(dealerSession.getStatus()).thenReturn(Status.GAME);
        when(dealerSession.isActive()).thenReturn(false);
        sessionsManager.join(new DealerJoinCommand("player1"));

        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad")))
                .withMessage("The session is not able to advertise");
    }
    
    @Test
    public void setAdDetachedWithAlias() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.setAdDetached(new AdCommandDetached("p1", 65, "ad"));
        
        verify(basicAfkSession).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void setAdDetachedIfSessionActive() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad"));
        
        verify(basicAfkSession).setAd(argThat(ad -> ad.getDuration() == 65 && ad.getText().equals("ad")));
    }
    
    @Test
    public void throwExceptionIfSettingAdDetachedIfNoSuchSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        
        assertThatExceptionOfType(SessionException.class)
                .isThrownBy(() -> sessionsManager.setAdDetached(new AdCommandDetached("player1", 65, "ad")))
                .withMessage("Cannot find a player - player1");
    }
    
    @Test
    public void leaveOnLeaveActiveIfSessionActive() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.leaveActive();
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void throwExceptionOnLeaveActiveIfNoActiveSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.leaveActive())
                .withMessage("There is no active session - nick or alias not provided");
    }
    
    @Test
    public void leaveOnLeaveDetachedIfNoActiveSession() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.leaveDetached(new LeaveCommandDetached("player1"));
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void leaveOnLeaveDetachedWithAlias() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        
        sessionsManager.leaveDetached(new LeaveCommandDetached("p1"));
        
        assertThat(sessionsManager.isAnyActive()).isFalse();
        assertThat(sessionsManager.getStatus()).isEmpty();
    }
    
    @Test
    public void throwExceptionOnLeaveDetachedIfSessionActive() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.leaveDetached(new LeaveCommandDetached("player2")))
                .withMessage("Cannot leave when attached to a session");
    }
    
    @Test
    public void sendMessageIfSessionActive() {
        when(basicAfkSession.getPlayer()).thenReturn(new Player("player1", "pass1", "p1"));
        when(basicAfkSession.getStatus()).thenReturn(Status.GAME);
        when(basicAfkSession.isActive()).thenReturn(false);
        sessionsManager.join(new JoinCommand("player1"));
        sessionsManager.attach(new AttachCommand("player1"));
        
        sessionsManager.sendMessage("message");
        
        verify(basicAfkSession).sendChatMessage("message");
    }
    
    @Test
    public void throwExceptionWhenSendMessageIfNoActiveSession() {        
        assertThatExceptionOfType(IllegalMethodInvocationException.class)
                .isThrownBy(() -> sessionsManager.sendMessage("message"))
                .withMessage("There is no active sessions to send a message");
    }
}
