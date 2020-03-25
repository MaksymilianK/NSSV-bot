package pl.konradmaksymilian.nssvbot.session;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Random;
import java.util.function.Consumer;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.KeepAlivePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.utils.Timer;

public abstract class Session {

    protected final ConnectionManager connection;
    protected final Random random = new Random();

    protected int playerEid;
    protected Consumer<Object> onMessage;
    protected Player player;
    protected Timer timer;
    protected Status status = Status.DISCONNECTED;
    protected boolean isActive = false;

    private String code = null;

    public Session(ConnectionManager connection, Timer timer) {
        this.connection = connection;
        this.timer = timer;
        setUpTimer();
    }
    
    public void joinServer(Player player, Consumer<Object> onMessage) {
        if (this.player != null || this.onMessage != null) {
            throw new IllegalMethodInvocationException("Cannot join the server once again");
        }
        this.player = player;
        this.onMessage = onMessage;
        
        var connectionBuilder = connection.connectionBuilder()
                .onEveryConnection(this::onEveryConnection)
                .onEveryCheck(this::onEveryCheck)
                .onEveryDisconnection(this::onEveryDisconnection)
                .onIncomingPacket(this::onPacket);
        try {
            connectionBuilder.connect();
        } catch (InterruptedException e) {
            onMessage.accept("Player '" + player.getNick() + "' has left the server");
        }
    }
    
    public void sendChatMessage(String message) {
        connection.sendPacket(new ChatMessageServerboundPacket(message));
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    protected void setUpTimer() {
        timer.setTimeToNow("lastKeepAlive");
        timer.setTimeFromNow("nextPossibleAttempt", Duration.ofMillis(500));
        timer.setDuration("keepAlive", Duration.ofSeconds(30));
    }
    
    protected void onEveryConnection() {
        timer.setTimeToNow("lastKeepAlive");
        connection.setCompression(new Compression(false, Integer.MAX_VALUE));
        connection.setState(State.HANDSHAKING);
        onMessage.accept("Player '" + player.getNick() + "' has connected to the server");
        this.join();
    }
    
    protected void onEveryCheck() {
        checkKeepAlive();
        checkStatus();
    }

    protected void checkStatus() {
        if (status.equals(Status.GAME) || !timer.isNowAfter("nextPossibleAttempt")) {
            return;
        } else if (status.equals(Status.LOGIN)) {
            sendChatMessage("/login " + player.getPassword());
            delayNextAttempt();
        } else if (status.equals(Status.HUB)) {
            sendChatMessage("/polacz reallife");
            delayNextAttempt();
        } else if (status.equals(Status.JOINING)) {
            if (code != null) {
                sendChatMessage(code.trim());
            }
            status = Status.HUB;
            delayNextAttempt();
        }
    }

    protected void onEveryDisconnection(AbstractMap.SimpleImmutableEntry<String, Integer> info) {
        changeStatus(Status.DISCONNECTED);
        if (!info.getKey().isEmpty()) {
            onMessage.accept("Player '" + player.getNick() + "' has lost connection: " + info.getKey());
        }
        onMessage.accept("Player '" + player.getNick() + "' will try to reconnect in " + info.getValue() + " seconds");
    }

    protected void onPacket(Packet packet) {
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
            case PLAYER_POSITION_AND_LOOK_CLIENTBOUND:
                onPlayerPositionAndLook((PlayerPositionAndLookClientboundPacket) packet);
                break;
            case RESPAWN:
                onRespawn((RespawnPacket) packet);
                break;
            case SET_COMPRESSION:
                onSetCompression((SetCompressionPacket) packet);
                break;
            default:
               //do nothing... just skip the packet
        }
    }

    protected void onKeepAlive(KeepAlivePacket packet) {
        timer.setTimeToNow("lastKeepAlive");
        connection.sendPacket(new KeepAliveServerboundPacket(packet.getKeepAliveId()));
    }

    protected void onChatMessage(ChatMessageClientboundPacket packet) {
        if (isActive) {
            onMessage.accept(packet.getMessage());
        }

        if (status.equals(Status.HUB) && packet.toString().endsWith("botem.")) {
            code = packet.getMessage().getComponents().get(5).getText();
            changeStatus(Status.JOINING);
        } else if (status.equals(Status.LOGIN) && packet.toString().endsWith("zalogowany.")) {
            changeStatus(Status.HUB);
        }
    }

    protected void onJoinGame(JoinGamePacket packet) {
        playerEid = packet.getPlayerEid();
        changeStatus(Status.LOGIN);
    }

    protected void onDisconnectPacket(DisconnectPacket packet) {
        onMessage.accept("Player '" + player.getNick() + "' has been disconnected by the server - reason: "
                + packet.getReason());

        connection.disconnect();
    }

    protected void onLoginSuccess() {
        connection.setState(State.PLAY);
        connection.sendPacket(ClientSettingsPacket.builder()
                .chatColours(true)
                .chatMode(0)
                .displayedSkinParts(0x00)
                .locale("pl_PL")
                .mainHand(1)
                .viewDistance(4)
                .build());
    }

    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        connection.sendPacket(new TeleportConfirmPacket(packet.getTeleportId()));
    }

    protected void onRespawn(RespawnPacket packet) {
        timer.setTimeToNow("lastKeepAlive");
        if (packet.getDimension() != 0) {
           return;
        }
        if (packet.getGamemode() == 0) {
           changeStatus(Status.GAME);
        } else if (packet.getGamemode() == 2) {
            changeStatus(Status.HUB);
        } else {
            throw new RuntimeException("Unexpected respawn packet - gamemode: " + packet.getGamemode());
        }
    }

    protected void changeStatus(Status newStatus) {
        status = newStatus;
        timer.setTimeFromNow("nextPossibleAttempt", Duration.ofMillis(1000));
    }

    private void join() {
        connection.sendPacket(HandshakePacket.builder()
                .protocolVersion(340)
                .serverAddress(ConnectionManager.HOST)
                .serverPort(ConnectionManager.PORT)
                .nextState(State.LOGIN.getId())
                .build());
        connection.setState(State.LOGIN);
        connection.sendPacket(new LoginStartPacket(player.getNick()));
    }

    private void checkKeepAlive() {
        if (timer.isNowAfterDuration("lastKeepAlive", "keepAlive")) {
            onMessage.accept("Server has not responded to player '" + player.getNick() + "' for "
                    + timer.getDuration("keepAlive").toSeconds() + " seconds");
            connection.disconnect();
        }
    }

    private void onSetCompression(SetCompressionPacket packet) {
        connection.setCompression(new Compression(true, packet.getThreshold()));
    }
    
    private void delayNextAttempt() {
        timer.setTimeFromNow("nextPossibleAttempt", Duration.ofSeconds(60 + random.nextInt(120)));
    }
}
