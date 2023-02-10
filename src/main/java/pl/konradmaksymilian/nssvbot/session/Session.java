package pl.konradmaksymilian.nssvbot.session;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Random;
import java.util.function.Consumer;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.Colour;
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

    protected double x;
    protected double feetY;
    protected double z;
    protected float yaw;
    protected float pitch;

    protected Consumer<Object> onMessage;
    protected Player player;
    protected Timer timer;
    protected Status status = Status.DISCONNECTED;
    protected boolean isActive = false;
    protected Integer windowId = null;

    private int windowCounter = 1;
    private byte[] slotData = null;

    public Session(ConnectionManager connection, Timer timer) {
        this.connection = connection;
        this.timer = timer;
        setUpTimer();
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

    protected void setUpTimer() {
        timer.setDuration("keepAlive", Duration.ofSeconds(60));
        timer.setDuration("loginCooldown", Duration.ofMillis(500));
        timer.setDuration("antiAntiAfkDelay", Duration.ofMinutes(1));
    }

    protected void onEveryConnection() {
        timer.setTimeToNow("lastKeepAlive");
        timer.setTimeToNow("nextPossibleLoginAttempt");
        connection.setCompression(new Compression(false, Integer.MAX_VALUE));
        connection.setState(State.HANDSHAKING);
        onMessage.accept("Player '" + player.getNick() + "' has connected to the server");
        this.join();
    }

    public void sendChatMessage(String message) {
        connection.sendPacket(new ChatMessageServerboundPacket(message));
    }

    protected void onEveryCheck() {
        checkKeepAlive();
        checkStatus();
        checkAntiAntiAfk();
    }

    protected void checkAntiAntiAfk() {
       // System.out.println(status);
        if (status.equals(Status.GAME) && timer.isNowAfter("nextAntiAntiAfk")) {
            delayNextAntiAntiAfk();
            //System.out.println((int) Math.floor(x) + " " + (int) Math.floor(feetY) + " " + (int) Math.floor(z));
            connection.sendPacket(PlayerBlockPlacementPacket.builder()
//                    .x(28438)
//                    .y(66)
//                    .z(-5448)
                    .x((int) Math.floor(x))
                    .y((int) Math.floor(feetY))
                    .z((int) Math.floor(z))
                    .face(1)
                    .hand(0)
                    .cursorX(0.5f)
                    .cursorY(0.5f)
                    .cursorZ(0.5f)
                    .build());
        }
    }

    protected void checkStatus() {
        if (!timer.isNowAfter("nextPossibleLoginAttempt")) {
            return;
        }

        if (status.equals(Status.LOGIN)) {
            sendChatMessage("/login " + player.getPassword());
            System.out.println("login");
            delayNextLoginAttempt();
        } else if (status.equals(Status.HUB) && slotData != null) {
            connection.sendPacket(new ClickWindowPacket(windowId, 37, 0, windowCounter, 0, slotData));
            windowCounter++;
            delayNextLoginAttempt();
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
                //onRespawn((RespawnPacket) packet);
                break;
            case OPEN_WINDOW:
                onOpenWindow((OpenWindowPacket) packet);
                break;
            case SET_SLOT:
                onSetSlot((SetSlotPacket) packet);
                break;
            case CONFIRM_TRANSACTION_CLIENTBOUND:
                onConfirmTransaction((ConfirmTransactionClientboundPacket) packet);
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
    }

    protected void onJoinGame(JoinGamePacket packet) {
        playerEid = packet.getPlayerEid();
        if (status.equals(Status.DISCONNECTED)) {
            changeStatus(Status.LOGIN);
            timer.setTimeFromNow("nextPossibleLoginAttempt", Duration.ofSeconds(1));
        } else if (status.equals(Status.HUB)) {
            changeStatus(Status.GAME);
            delayNextAntiAntiAfk();
        } else if (status.equals(Status.GAME)) {
            changeStatus(Status.HUB);
        }
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
        x = packet.getX();
        feetY = (int) packet.getFeetY();
        z = packet.getZ();

        if (packet.getFlags() == (byte) 0) {
            yaw = packet.getYaw();
            pitch = packet.getPitch();
        }
    }

//    protected void onRespawn(RespawnPacket packet) {
//        timer.setTimeToNow("lastKeepAlive");
//        System.out.println(packet.getDimension() + " " + packet.getGamemode());
//        if (status.equals(Status.HUB) && packet.getGamemode() == 0) {
//            changeStatus(Status.GAME);
//            delayNextAntiAntiAfk();
//            windowId = null;
//            slotData = null;
//        }
//    }

    protected void onOpenWindow(OpenWindowPacket packet) {
        if (status.equals(Status.LOGIN)) {
            changeStatus(Status.HUB);
            windowId = packet.getId();
            slotData = null;
        }
    }

    protected void onConfirmTransaction(ConfirmTransactionClientboundPacket packet) {
        if (!packet.isAccepted()) {
            connection.sendPacket(new ConfirmTransactionServerboundPacket(packet.getWindowId(),
                    packet.getActionNumber(), true));
        }
    }

    protected void onSetSlot(SetSlotPacket packet) {
        if (status.equals(Status.HUB) && packet.getSlot() == 28) {
            slotData = packet.getSlotData();
        }
    }

    protected void changeStatus(Status newStatus) {
        status = newStatus;
        timer.setTimeFromNow("nextPossibleLoginAttempt", "loginCooldown");
    }

    protected boolean isNsMessage(ChatMessage message) {
        return message.getComponents().size() > 4
                && message.getComponents().get(1).getStyle().getColour().isPresent()
                && message.getComponents().get(1).getStyle().getColour().get().equals(Colour.GOLD.getName())
                && message.getComponents().get(1).getText().equals("N")
                && message.getComponents().get(2).getStyle().getColour().isPresent()
                && message.getComponents().get(2).getStyle().getColour().get().equals(Colour.YELLOW.getName())
                && message.getComponents().get(2).getText().equals("S");
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
            changeStatus(Status.DISCONNECTED);
            connection.disconnect();
        }
    }

    private void onSetCompression(SetCompressionPacket packet) {
        connection.setCompression(new Compression(true, packet.getThreshold()));
    }

    private void delayNextLoginAttempt() {
        timer.setTimeFromNow("nextPossibleLoginAttempt", Duration.ofSeconds(10 + random.nextInt(10)));
    }

    private void delayNextAntiAntiAfk() {
        timer.setTimeFromNow("nextAntiAntiAfk", "antiAntiAfkDelay");
    }
}
