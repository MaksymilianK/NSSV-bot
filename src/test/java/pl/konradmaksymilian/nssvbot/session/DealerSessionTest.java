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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.config.DealerConfig;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager.ConnectionBuilder;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.protocol.ChatComponent;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ChatMessageServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ClientSettingsPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.KeepAliveServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.LoginStartPacket;
import pl.konradmaksymilian.nssvbot.utils.Timer;

public class DealerSessionTest {

    private DealerSession session;

    @Mock
    private ConnectionManager connection;

    @Mock
    private ConnectionBuilder connectionBuilder;

    @Mock
    private Timer timer;

    @Mock
    private Consumer<Object> onMessage;

    private Runnable onEveryCheck;

    private Runnable onEveryConnection;

    private Consumer<AbstractMap.SimpleImmutableEntry<String, Integer>> onEveryDisconnection;

    private Consumer<Packet> onIncomingPacket;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(connection.connectionBuilder()).thenReturn(connectionBuilder);

        when(connectionBuilder.onEveryCheck(any())).thenAnswer(invocation -> {
            onEveryCheck = invocation.getArgument(0);
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
        when(timer.isNowAfterDuration(any(), any(Duration.class))).thenReturn(false);

        var config = new DealerConfig("/p home 1", "/home s");

        session = new DealerSession(connection, timer, config);
        session.joinServer(new Player("GeniuszMistrz", "password", "g"), onMessage);

        onIncomingPacket.accept(PlayerPositionAndLookClientboundPacket.builder()
                .x(5)
                .feetY(2)
                .z(7)
                .yaw(135)
                .pitch(42)
                .teleportId(12)
                .build()
        );
    }

    //MovableSession tests:

    @Test
    public void setPositionOnPlayerPositionAndLook() {
        assertThat(session.x).isEqualTo(5);
        assertThat(session.feetY).isEqualTo(2);
        assertThat(session.z).isEqualTo(7);
    }

    @Test
    public void moveOneStep() {
        session.setNewDestination(10, 1);
        onEveryCheck.run();

        assertThat(session.x).isGreaterThan(5);
        assertThat(session.feetY).isEqualTo(2);
        assertThat(session.z).isLessThan(7);
        double distanceSquare = Math.pow(session.x - 5, 2) + Math.pow(session.z - 7, 2);
        assertThat(distanceSquare).isLessThan(Math.pow(MovableSession.MAX_MOVE + 0.01d, 2));
        assertThat(distanceSquare).isGreaterThan(Math.pow(MovableSession.MAX_MOVE - 0.01d, 2));
    }

    @Test
    public void stopAtDestinationPosition() {
        session.setNewDestination(10, 1);
        for (int i = 0; i < 100; i++) {
            onEveryCheck.run();
        }

        assertThat(session.x).isEqualTo(10);
        assertThat(session.feetY).isEqualTo(2);
        assertThat(session.z).isEqualTo(1);
    }

    @Test
    public void resetPositionDuringMove() {
        session.setNewDestination(10, 1);
        onEveryCheck.run();
        onEveryCheck.run();
        onEveryCheck.run();

        onIncomingPacket.accept(PlayerPositionAndLookClientboundPacket.builder()
                .x(5)
                .feetY(2)
                .z(7)
                .yaw(135)
                .pitch(42)
                .teleportId(12)
                .build()
        );

        onEveryCheck.run();

        assertThat(session.x).isGreaterThan(5);
        assertThat(session.feetY).isEqualTo(2);
        assertThat(session.z).isLessThan(7);
        double distanceSquare = Math.pow(session.x - 5, 2) + Math.pow(session.z - 7, 2);
        assertThat(distanceSquare).isLessThan(Math.pow(MovableSession.MAX_MOVE + 0.01d, 2));
        assertThat(distanceSquare).isGreaterThan(Math.pow(MovableSession.MAX_MOVE - 0.01d, 2));
    }

    @Test
    public void callbackOnMoveFinish() {
        Runnable onMoveFinish = Mockito.mock(Runnable.class);

        session.setNewDestination(6, 8, onMoveFinish);
        for (int i = 0; i < 10; i++) {
            onEveryCheck.run();
        }

        verify(onMoveFinish, times(1)).run();
    }

    //Session tests:

    @Test
    public void joiningServer() throws InterruptedException {
        verify(connectionBuilder).connect();
    }

    @Test
    public void setUpOnConnectionStart() {
        onEveryConnection.run();

        assertThat(session.getStatus()).isEqualTo(Status.DISCONNECTED);
        assertThat(session.isActive()).isFalse();
        verify(connection).setCompression(new Compression(false, Integer.MAX_VALUE));
        verify(connection).setState(State.HANDSHAKING);
    }

    @Test
    public void sendHandshakeOnConnectionStart() {
        onEveryConnection.run();

        verify(connection).sendPacket(HandshakePacket.builder()
                .nextState(2)
                .protocolVersion(340)
                .serverAddress("nssv.pl")
                .serverPort((short) 25565)
                .build());
    }

    @Test
    public void sendInternalMessageOnConnectionStart() {
        onEveryConnection.run();

        verify(onMessage).accept("Player 'GeniuszMistrz' has connected to the server");
    }

    @Test
    public void setNewStateAfterHandshake() {
        onEveryConnection.run();

        verify(connection).sendPacket(new LoginStartPacket("GeniuszMistrz"));
    }

    @Test
    public void setLoginStateAfterHandshake() {
        onEveryConnection.run();

        verify(connection).setState(State.LOGIN);
    }

    @Test
    public void sendLoginStartAfterHandshake() {
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
    public void sendInternalMessageOnCheckIfKeepAliveNotReceivedForLongTime() {
        when(timer.isNowAfterDuration("lastKeepAlive", "keepAlive")).thenReturn(true);
        when(timer.getDuration("keepAlive")).thenReturn(Duration.ofSeconds(20));

        onEveryCheck.run();

        verify(onMessage).accept("Server has not responded to player 'GeniuszMistrz' for 20 seconds");
    }

    @Test
    public void disconnectOnCheckIfKeepAliveNotReceivedForLongTime() {
        when(timer.isNowAfterDuration("lastKeepAlive", "keepAlive")).thenReturn(true);
        when(timer.getDuration("keepAlive")).thenReturn(Duration.ofSeconds(20));

        onEveryCheck.run();

        verify(connection).disconnect();
    }

    @Test
    public void doNothingWithAdvertIfAdvertNotSet() {
        when(timer.isNowAfterDuration("lastAdvertising", "advertising")).thenReturn(true);

        onEveryCheck.run();

        verify(connection, never()).sendPacket(new ChatMessageServerboundPacket("advert"));
        verify(timer, never()).setTimeToNow("lastAdvertising");
    }

    @Test
    public void tryToLogInAndDelayNextAttemptIfItIsTimeToAndStatusLogin() {
        when(timer.isNowAfter("nextPossibleAttempt")).thenReturn(true);
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN

        onEveryCheck.run();
        onEveryCheck.run();
        onEveryCheck.run();

        verify(connection, times(3)).sendPacket(new ChatMessageServerboundPacket("/login password"));
        verify(timer, times(5)).setTimeFromNow(eq("nextPossibleAttempt"), argThat((Duration d) -> !d.isNegative()));
    }

    @Test
    public void tryToJoinModeAndDelayNextAttemptIfItIsTimeToAndStatusLogin() {
        when(timer.isNowAfter("nextPossibleAttempt")).thenReturn(true);
        onIncomingPacket.accept(new JoinGamePacket()); //in order to set status to LOGIN
        onIncomingPacket.accept(new RespawnPacket(0, 2)); //in order to set status to HUB

        onEveryCheck.run();
        onEveryCheck.run();
        onEveryCheck.run();

        verify(connection, times(3)).sendPacket(new ChatMessageServerboundPacket("/polacz reallife"));
        verify(timer, times(6)).setTimeFromNow(eq("nextPossibleAttempt"), argThat((Duration d) -> !d.isNegative()));
    }
}
