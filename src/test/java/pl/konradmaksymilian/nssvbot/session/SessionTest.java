package pl.konradmaksymilian.nssvbot.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager.ConnectionBuilder;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.protocol.ChatComponent;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectLoginPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectPlayPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.JoinGamePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.KeepAliveClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.LoginSuccessPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.RespawnPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ChatMessageServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ClientSettingsPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.KeepAliveServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.LoginStartPacket;

public class SessionTest {

    private Session session;
    
    @Mock
    private ConnectionManager connection;
    
    @Mock
    private ConnectionBuilder connectionBuilder;
    
    @Mock
    private Timer timer;
    
    @Mock
    private Consumer<Object> onMessage;
    
    private Runnable onEveryCheckFinish;
    
    private Runnable onEveryConnection;

    private Consumer<AbstractMap.SimpleImmutableEntry<String, Integer>> onEveryDisconnection;

    private Consumer<Packet> onIncomingPacket;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(connection.connectionBuilder()).thenReturn(connectionBuilder);
        
        when(connectionBuilder.onEveryCheckFinish(any())).thenAnswer(invocation -> {
            onEveryCheckFinish = invocation.getArgument(0);
            return connectionBuilder;
        });
        
        when(connectionBuilder.onEveryConnection(any())).thenAnswer(invocation -> {
            onEveryConnection = invocation.getArgument(0);
            return connectionBuilder;
        });
        
        when(connectionBuilder.onEveryDisconnection(any())).thenAnswer(invocation -> {
            onEveryDisconnection = invocation.getArgument(0);
            return connectionBuilder;
        });
        
        when(connectionBuilder.onIncomingPacket(any())).thenAnswer(invocation -> {
            onIncomingPacket = invocation.getArgument(0);
            return connectionBuilder;
        });
        
        when(timer.isNowAfter(any())).thenReturn(false);
        when(timer.isNowAfterDuration(any(), any(String.class))).thenReturn(false);
        when(timer.isNowAfterDuration(any(), any(Duration.class))).thenReturn(false);
        
        session = new Session(connection, timer);
        session.joinServer(new Player("GeniuszMistrz", "password", "g"), onMessage);
    }
    
    @Test
    public void joiningServer() throws InterruptedException {        
        verify(connectionBuilder).connect();
    }
    
    @Test
    public void setUpOnConnectionStart() throws InterruptedException {        
        onEveryConnection.run();
        
        assertThat(session.getStatus()).isEqualTo(Status.DISCONNECTED);
        assertThat(session.isActive()).isFalse();
        verify(connection).setCompression(new Compression(false, Integer.MAX_VALUE));
        verify(connection).setState(State.HANDSHAKING);
    }
    
    @Test
    public void sendHandshakeOnConnectionStart() throws InterruptedException {        
        onEveryConnection.run();
        
        verify(connection).sendPacket(HandshakePacket.builder()
                .nextState(2)
                .protocolVersion(340)
                .serverAddress("nssv.pl")
                .serverPort((short) 25565)
                .build());
    }

    @Test
    public void sendInternalMessageOnConnectionStart() throws InterruptedException {
        onEveryConnection.run();

        verify(onMessage).accept("Player 'GeniuszMistrz' has connected to the server");
    }
    
    @Test
    public void setNewStateAfterHandshake() throws InterruptedException {        
        onEveryConnection.run();
        
        verify(connection).sendPacket(new LoginStartPacket("GeniuszMistrz"));
    }
    
    @Test
    public void setLoginStateAfterHandshake() throws InterruptedException {        
        onEveryConnection.run();
        
        verify(connection).setState(State.LOGIN);
    }
    
    @Test
    public void sendLoginStartAfterHandshake() throws InterruptedException {        
        onEveryConnection.run();
        
        verify(connection).sendPacket(new LoginStartPacket("GeniuszMistrz"));
    }
    
    @Test
    public void respondWithKeepAliveOnKeepAlive() {
        onIncomingPacket.accept(new KeepAliveClientboundPacket(15L));
        
        verify(connection).sendPacket(new KeepAliveServerboundPacket(15L));
    }
    
    @Test
    public void setStatusLoginOnJoinGamePacket() {
        onIncomingPacket.accept(new JoinGamePacket());
        
        assertThat(session.getStatus()).isEqualTo(Status.LOGIN);
    }
    
    @Test
    public void passMessageOnChatMessagePacketIfActive() {
        session.setActive(true);
        var component = ChatComponent.builder()
                .text("message")
                .bold(false)
                .italic(false)
                .obfuscated(false)
                .strikethrough(false)
                .underlined(false)
                .build();
        var chatMessage = ChatMessage.builder().append(component).build();
        
        onIncomingPacket.accept(new ChatMessageClientboundPacket(chatMessage));
        
        verify(onMessage).accept(chatMessage);
    }
    
    @Test
    public void doNotPassMessageOnChatMessagePacketIfActive() {
        var component = ChatComponent.builder()
                .text("message")
                .bold(false)
                .italic(false)
                .obfuscated(false)
                .strikethrough(false)
                .underlined(false)
                .build();
        var chatMessage = ChatMessage.builder().append(component).build();
        
        onIncomingPacket.accept(new ChatMessageClientboundPacket(chatMessage));
        
        verify(onMessage, never()).accept(any());
    }
    
    @Test
    public void changeStatusToHubIfStatusLoginAndChatMessageStartsWithPhraseAndHasColour() {
        var component = ChatComponent.builder()
                .text("Zalogowano. absfkfgasdksfsafojsf")        //text must start with "Zalogowano."
                .colour("green")
                .bold(true)             //styles are random - they don't matter except for the colour
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var chatMessage = ChatMessage.builder().append(component).build();
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN

        onIncomingPacket.accept(new ChatMessageClientboundPacket(chatMessage));

        assertThat(session.getStatus()).isEqualTo(Status.HUB);
    }
    
    @Test
    public void doNotChangeStatusIfChatMessageDoesNotStartWithPhrase() {
        var component = ChatComponent.builder()
                .text("Abcdef. absfkfgasdksfsafojsf")
                .colour("green")
                .bold(true)             //styles are random - they don't matter except for the colour
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var chatMessage = ChatMessage.builder().append(component).build();
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN

        onIncomingPacket.accept(new ChatMessageClientboundPacket(chatMessage));

        assertThat(session.getStatus()).isEqualTo(Status.LOGIN);
    }
    
    @Test
    public void doNotChangeSatusIfChatMessageDoNotHaveColour() {
        var component = ChatComponent.builder()
                .text("Abcdef. absfkfgasdksfsafojsf")
                .bold(true)             //styles are random - they don't matter except for the colour
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var chatMessage = ChatMessage.builder().append(component).build();
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN

        onIncomingPacket.accept(new ChatMessageClientboundPacket(chatMessage));

        assertThat(session.getStatus()).isEqualTo(Status.LOGIN);
    }
    
    @Test
    public void sendInternalMessageOnDisconnectPlayPacket() {
        var component = ChatComponent.builder()
                .text("disconnect reason")
                .bold(true)             //styles are random
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var reasonMessage = ChatMessage.builder().append(component).build();
        
        onIncomingPacket.accept(new DisconnectPlayPacket(reasonMessage));

        verify(onMessage).accept("Player 'GeniuszMistrz' has been disconnected by the server - reason: disconnect reason");
    }
    
    @Test
    public void sendInternalMessageOnDisconnectLoginPacket() {
        var component = ChatComponent.builder()
                .text("disconnect reason")
                .bold(true)             //styles are random
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var reasonMessage = ChatMessage.builder().append(component).build();
        
        onIncomingPacket.accept(new DisconnectLoginPacket(reasonMessage));

        verify(onMessage).accept("Player 'GeniuszMistrz' has been disconnected by the server - reason: disconnect reason");
    }

    @Test
    public void sendInternalMessageWithReasonAndTimeOnDisconnectionIfThereIsReason() {
        onEveryDisconnection.accept(new AbstractMap.SimpleImmutableEntry<>("disconnect reason", 150));

        verify(onMessage).accept("Player 'GeniuszMistrz' has lost connection: disconnect reason");
        verify(onMessage).accept("Player 'GeniuszMistrz' will try to reconnect in 150 seconds");
    }

    @Test
    public void sendInternalMessageWithOnlyTimeOnDisconnectionIfThereIsNoReason() {
        onEveryDisconnection.accept(new AbstractMap.SimpleImmutableEntry<>("", 150));

        verify(onMessage, never()).accept("Player 'GeniuszMistrz' has lost connection: ");
        verify(onMessage).accept("Player 'GeniuszMistrz' will try to reconnect in 150 seconds");
    }
    
    @Test
    public void disconnectOnDisconnectPlayPacket() {
        var component = ChatComponent.builder()
                .text("disconnect reason")
                .bold(true)             //styles are random
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var reasonMessage = ChatMessage.builder().append(component).build();
        
        onIncomingPacket.accept(new DisconnectPlayPacket(reasonMessage));
        
        verify(connection).disconnect();
    }
    
    @Test
    public void disconnectOnDisconnectLoginPacket() {
        var component = ChatComponent.builder()
                .text("disconnect reason")
                .bold(true)             //styles are random
                .italic(false)
                .obfuscated(true)
                .strikethrough(false)
                .underlined(false)
                .build();
        var reasonMessage = ChatMessage.builder().append(component).build();
        
        onIncomingPacket.accept(new DisconnectLoginPacket(reasonMessage));
        
        verify(connection).disconnect();
    }
    
    @Test
    public void setStatePlayOnLoginSuccess() {
        onIncomingPacket.accept(new LoginSuccessPacket("123-456", "GeniuszMistrz"));
        
        verify(connection).setState(State.PLAY);
    }
    
    @Test
    public void sendClientSettingsOnLoginSuccess() {
        onIncomingPacket.accept(new LoginSuccessPacket("123-456", "GeniuszMistrz"));
        
        verify(connection).sendPacket(any(ClientSettingsPacket.class));
    }
    
    @Test
    public void doNothingOnRespawnIfDimensionNotEqualTo0() {
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN
        onIncomingPacket.accept(new RespawnPacket(1, 0));
        
        assertThat(session.getStatus()).isEqualTo(Status.LOGIN);
    }
    
    @Test
    public void setStatusGameOnRespawnIfDimensionEqualTo0AndGamemodeEqualTo0() {
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN
        onIncomingPacket.accept(new RespawnPacket(0, 0));
        
        assertThat(session.getStatus()).isEqualTo(Status.GAME);
    }
    
    @Test
    public void setStatusHubOnRespawnIfDimensionEqualTo0AndGamemodeEqualTo2() {
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN
        onIncomingPacket.accept(new RespawnPacket(0, 2));
        
        assertThat(session.getStatus()).isEqualTo(Status.HUB);
    }
    
    @Test
    public void sendAdvertOnAdvertSetIfDurationPositiveAndNotLessThan60s() {
        session.setAd(new Advert(60, "advert"));
        
        verify(connection).sendPacket(new ChatMessageServerboundPacket("advert"));
    }
    
    @Test
    public void cancelAdvertOnAdvertSetIfDurationNegative() {
        session.setAd(new Advert(-1, "advert"));
        
        verify(connection, never()).sendPacket(any());
    }
    
    @Test
    public void sendInternalMessageOnCheckIfKeepAliveNotReceivedForLongTime() {
        when(timer.isNowAfterDuration("lastKeepAlive", "keepAlive")).thenReturn(true);
        when(timer.getDuration("keepAlive")).thenReturn(Duration.ofSeconds(20));
        
        onEveryCheckFinish.run();
        
        verify(onMessage).accept("Server has not responded to player 'GeniuszMistrz' for 20 seconds");
    }
    
    @Test
    public void disconnectOnCheckIfKeepAliveNotReceivedForLongTime() {
        when(timer.isNowAfterDuration("lastKeepAlive", "keepAlive")).thenReturn(true);
        when(timer.getDuration("keepAlive")).thenReturn(Duration.ofSeconds(20));
        
        onEveryCheckFinish.run();
        
        verify(connection).disconnect();
    }
    
    @Test
    public void sendAdverAndSetNewTimeWheneverItIsTimeToAndAdvertSet() {
        session.setAd(new Advert(120, "advert"));
        when(timer.isNowAfterDuration("lastAdvertising", "advertising")).thenReturn(true);
        
        onEveryCheckFinish.run();
        onEveryCheckFinish.run();
        onEveryCheckFinish.run();
        
        verify(connection, times(4)).sendPacket(new ChatMessageServerboundPacket("advert")); // once on advert set and 3 times on every check
        verify(timer, times(4)).setTimeToNow("lastAdvertising");
    }

    @Test
    public void doNothingWithAdvertIfAdvertNotSet() {
        when(timer.isNowAfterDuration("lastAdvertising", "advertising")).thenReturn(true);
        
        onEveryCheckFinish.run();
        
        verify(connection, never()).sendPacket(new ChatMessageServerboundPacket("advert"));
        verify(timer, never()).setTimeToNow("lastAdvertising");
    }
    
    @Test
    public void doNothingIfItIsNotTimeToSendNextAdvert() {
        session.setAd(new Advert(120, "advert"));
        when(timer.isNowAfterDuration("lastAdvertising", "advertising")).thenReturn(false);
        
        onEveryCheckFinish.run();
        
        verify(connection, times(1)).sendPacket(new ChatMessageServerboundPacket("advert")); // once on advert set
        verify(timer, times(1)).setTimeToNow("lastAdvertising"); // once on advert set
    }
    
    @Test
    public void tryToLogInAndDelayNextAttemptIfItIsTimeToAndStatusLogin() {
        when(timer.isNowAfter("nextPossibleAttempt")).thenReturn(true);
        var time = Instant.MAX.minus(Duration.ofMinutes(10));
        when(timer.getNow()).thenReturn(time);
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN

        onEveryCheckFinish.run();
        onEveryCheckFinish.run();
        onEveryCheckFinish.run();
        
        verify(connection, times(3)).sendPacket(new ChatMessageServerboundPacket("/login password"));
        verify(timer, times(3)).setTime(eq("nextPossibleAttempt"), argThat((Instant i) -> i.isAfter(time)));
    }
    
    @Test
    public void tryToJoinModeAndDelayNextAttemptIfItIsTimeToAndStatusLogin() {
        when(timer.isNowAfter("nextPossibleAttempt")).thenReturn(true);
        var time = Instant.MAX.minus(Duration.ofMinutes(10));
        when(timer.getNow()).thenReturn(time);
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN
        onIncomingPacket.accept(new RespawnPacket(0, 2)); //in order to set status to HUB

        onEveryCheckFinish.run();
        onEveryCheckFinish.run();
        onEveryCheckFinish.run();
        
        verify(connection, times(3)).sendPacket(new ChatMessageServerboundPacket("/polacz reallife"));
        verify(timer, times(3)).setTime(eq("nextPossibleAttempt"), argThat((Instant i) -> i.isAfter(time)));
    }
}
