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

    private final ConnectionManager connection;
    private final Random random = new Random();
   
    private Consumer<Object> onMessage;
    private Player player;
    private Timer timer;
    private Status status = Status.DISCONNECTED;
    private Advert advert;
    private boolean isActive = false;
    
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
                .onEveryCheckFinish(this::onEveryCheck)
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
    
    public void setAd(Advert advert) {
        if (advert.getDuration() < 0) {
            this.advert = null;
        } else if (advert.getDuration() >= 60) {
            this.advert = advert;
            timer.setTimeToNow("lastAdvertising");
            timer.setDuration("advertising", Duration.ofSeconds(advert.getDuration()));
            sendChatMessage(advert.getText());
        } else {
            throw new IllegalArgumentException("Advert cannot have positive duration that is less than 60s");
        }
    }
    
    private void setUpTimer() {
        timer.setTimeToNow("lastKeepAlive");
        timer.setDuration("keepAlive", Duration.ofSeconds(20));
        timer.setTimeToNow("nextPossibleAttempt");
    }
    
    private void onEveryConnection() {
        timer.setTimeToNow("lastKeepAlive");
        connection.setCompression(new Compression(false, Integer.MAX_VALUE));
        connection.setState(State.HANDSHAKING);
        onMessage.accept("Player '" + player.getNick() + "' has connected to the server");
        this.join();
    }
    
    private void onEveryCheck() {
        checkKeepAlive();
        checkAdvert();
        checkStatus();
    }
    
    private void checkKeepAlive() {
        if (timer.isNowAfterDuration("lastKeepAlive", "keepAlive")) {
            onMessage.accept("Server has not responded to player '" + player.getNick() + "' for "
                    + timer.getDuration("keepAlive").toSeconds() + " seconds");
            connection.disconnect();
        }
    }
    
    private void checkAdvert() {
        if (advert != null) {
            if (timer.isNowAfterDuration("lastAdvertising", "advertising")) {
                timer.setTimeToNow("lastAdvertising");
                sendChatMessage(advert.getText());
            }
        }
    }
    
    private void checkStatus() {
        if (status.equals(Status.GAME) || !timer.isNowAfter("nextPossibleAttempt")) {
            return;
        } else if (status.equals(Status.LOGIN)) {
            delayNextAttempt();
            sendChatMessage("/login " + player.getPassword());
        } else if (status.equals(Status.HUB)) {
            delayNextAttempt();
            sendChatMessage("/polacz reallife");
        }
    }
    
    private void onEveryDisconnection(AbstractMap.SimpleImmutableEntry<String, Integer> info) {
        changeStatus(Status.DISCONNECTED);
        if (!info.getKey().isEmpty()) {
            onMessage.accept("Player '" + player.getNick() + "' has lost connection: " + info.getKey());
        }
        onMessage.accept("Player '" + player.getNick() + "' will try to reconnect in " + info.getValue() + " seconds");
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
            default:
               //do nothing... just skip the packet
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
        connection.sendPacket(new LoginStartPacket(player.getNick()));
    }
    
    private void onKeepAlive(KeepAlivePacket packet) {
        timer.setTimeToNow("lastKeepAlive");
        connection.sendPacket(new KeepAliveServerboundPacket(packet.getKeepAliveId()));
    }
    
    private void onChatMessage(ChatMessageClientboundPacket packet) {
        if (isActive) {
            onMessage.accept(packet.getMessage());
        }
        
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
        onMessage.accept("Player '" + player.getNick() + "' has been disconnected by the server - reason: "
                + packet.getReason());

        connection.disconnect();
    }
    
    private void onLoginSuccess() {
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
    
    private void onPlayerPositionAndLook(PlayerPositionAndLookPacket packet) {
        connection.sendPacket(new TeleportConfirmPacket(packet.getTeleportId()));
    }
    
    private void onRespawn(RespawnPacket packet) {
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
    
    private void onSetCompression(SetCompressionPacket packet) {
        connection.setCompression(new Compression(true, packet.getThreshold()));
    }
    
    private void changeStatus(Status newStatus) {
        status = newStatus;
        timer.setTimeToNow("nextPossibleAttempt");
    }
    
    private void delayNextAttempt() {
        timer.setTime("nextPossibleAttempt", timer.getNow().plusSeconds(60 + random.nextInt(120)));
    }
}
