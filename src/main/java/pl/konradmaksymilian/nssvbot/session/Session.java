package pl.konradmaksymilian.nssvbot.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.KeepAlivePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.JoinGamePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.RespawnPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.SetCompressionPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ChatMessageServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ClientSettingsPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.KeepAliveServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.LoginStartPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.TeleportConfirmPacket;

public class Session {

    private final Logger logger = LoggerFactory.getLogger(Session.class);
    private final ConnectionManager connection;
    private final String nick;
    private final String password;
    private final Random random = new Random();
    
    private Instant lastKeepAlive;
    private Instant nextPossibleAttempt = Instant.now();
    private Status status = Status.DISCONNECTED;
    
    public Session(ConnectionManager connection, String nick, String password) {
        this.connection = connection;
        this.nick = nick;
        this.password = password;
        joinServer();
    }
    
    private void joinServer() {
        var connectionBuilder = connection.connectionBuilder()
                .onEveryConnection(this::onConnection)
                .onEveryCheckFinish(this::onEveryCheck)
                .onEveryDisconnection(this::onDisconnection)
                .onIncomingPacket(this::onPacket);
        try {
            connectionBuilder.connect();
        } catch (InterruptedException e) {
            logger.info("The session has been closed!");
        }
    }
    
    private void onConnection() {
        lastKeepAlive = Instant.now();
        connection.setCompression(new Compression(false, Integer.MAX_VALUE));
        connection.setState(State.HANDSHAKING);
        this.join();
    }
    
    private void onEveryCheck() {
        var now = Instant.now();
        if (Duration.between(lastKeepAlive, now).toSeconds() > 20) {
            logger.info("Server has not responded for 20s");
            connection.disconnect();
        }
        
        if (status.equals(Status.GAME) || !isAttemptPossible(now)) {
            return;
        } else if (status.equals(Status.LOGIN)) {
            delayNextAttempt();
            connection.sendPacket(new ChatMessageServerboundPacket("/login " + password));
        } else if (status.equals(Status.HUB)) {
            delayNextAttempt();
            connection.sendPacket(new ChatMessageServerboundPacket("/polacz reallife"));
        }
    }
    
    private void onDisconnection() {
        changeStatus(Status.DISCONNECTED);
    }
    
    private void onPacket(Packet packet) {
        switch (packet.getName()) {
            case KEEP_ALIVE_CLIENTBOUND:
                onKeepAlive((KeepAlivePacket) packet);
                break;
            case CHAT_MESSAGE_CLIENTBOUND:
                onChatMessage((ChatMessageClientboundPacket) packet);
                break;
            case DISCONNECT_LOGIN: case DISCONNECT_PLAY:
                onDisconnectPacket((DisconnectPacket) packet);
                break;
            case LOGIN_SUCCESS:
                onLoginSuccess();
                break;
            case JOIN_GAME:
                onJoinGame((JoinGamePacket) packet);
                break;
            case PLAYER_POSITION_AND_LOOK:
                onPlayerPositionAndLook((PlayerPositionAndLookPacket) packet);
                break;
            case RESPAWN:
                onRespawn((RespawnPacket) packet);
                break;
            case SET_COMPRESSION:
                onSetCompression((SetCompressionPacket) packet);
                break;
            case PLUGIN_MESSAGE_CLIENTBOUND:
                logger.info(packet.toString());
            default:
               // throw new UnrecognizedPacketException("The packet: " + packet.getName() + " has not been recognized");
        }
    }
    
    private void join() {
        connection.sendPacket(HandshakePacket.builder()
                .protocolVersion(340)
                .serverAddress(ConnectionManager.HOST)
                .serverPort(ConnectionManager.PORT)
                .nextState(State.LOGIN.getId())
                .build());
        connection.setState(State.LOGIN);
        connection.sendPacket(new LoginStartPacket(nick));
    }
    
    private void onKeepAlive(KeepAlivePacket packet) {
        lastKeepAlive = Instant.now();
        connection.sendPacket(new KeepAliveServerboundPacket(packet.getKeepAliveId()));
    }
    
    private void onChatMessage(ChatMessageClientboundPacket packet) {
        System.out.println(packet.toString());
        if (status.equals(Status.LOGIN)) {
            var firstPart = packet.getMessage().getComponents().get(0);
            if (firstPart.getStyle().getColour().isPresent() && firstPart.getText().startsWith("Zalogowano.")) {
                changeStatus(Status.HUB);
            }
        }
    }
    
    private void onJoinGame(JoinGamePacket packet) {
        changeStatus(Status.LOGIN);
    }
    
    private void onDisconnectPacket(DisconnectPacket packet) {
        logger.info("Disconnected by the server - reason: " + packet.getReason());
        connection.disconnect();
    }
    
    private void onLoginSuccess() {
        connection.setState(State.PLAY);
        connection.sendPacket(ClientSettingsPacket.builder()
                .chatColours(true)
                .chatMode(0)
                .displayedSkinParts(0)
                .locale("pl_PL")
                .mainHand(1)
                .viewDistance(4)
                .build());
    }
    
    private void onPlayerPositionAndLook(PlayerPositionAndLookPacket packet) {
        connection.sendPacket(new TeleportConfirmPacket(packet.getTeleportId()));
    }
    
    private void onRespawn(RespawnPacket packet) {
        lastKeepAlive = Instant.now();
        if (packet.getDimension() != 0) {
           return;
        }
        
        if (packet.getGamemode() == 0) {
           changeStatus(Status.GAME);
        } else if (packet.getGamemode() == 2) {
            changeStatus(Status.HUB);
        } else {
            throw new SessionException("Unexpected respawn packet - gamemode: " + packet.getGamemode());
        }
    }
    
    private void onSetCompression(SetCompressionPacket packet) {
        connection.setCompression(new Compression(true, packet.getThreshold()));
    }
    
    private void changeStatus(Status newStatus) {
        status = newStatus;
        nextPossibleAttempt = Instant.now();
    }
    
    private boolean isAttemptPossible(Instant time) {
        return time.isAfter(nextPossibleAttempt);
    }
    
    private void delayNextAttempt() {
        nextPossibleAttempt = Instant.now().plusSeconds(60 + random.nextInt(120));
    }
}
