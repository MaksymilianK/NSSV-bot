package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.management.Player;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.utils.Timer;

import java.time.Duration;
import java.util.Random;

public abstract class MovableSession extends Session {

    public static final double MAX_MOVE = 0.25d;

    protected double x;
    protected double feetY;
    protected double z;
    protected float yaw;
    protected float pitch;
    private HorizontalMove move;

    public MovableSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
    }

    public void setNewDestination(double newX, double newZ) {
        this.move = HorizontalMove.builder()
                .currentX(x)
                .currentZ(z)
                .destinationX(newX)
                .destinationZ(newZ)
                .build();
    }

    public void setNewDestination(double newX, double newZ, float yaw, float pitch) {
        this.move = HorizontalMove.builder()
                .currentX(x)
                .currentZ(z)
                .destinationX(newX)
                .destinationZ(newZ)
                .yaw(yaw)
                .pitch(pitch)
                .build();
    }

    public void removeDestination() {
        move = null;
    }

    public boolean isMoving() {
        return !reachedDestination() || !looksWhereShould();
    }

    @Override
    protected void onPacket(Packet packet) {
        super.onPacket(packet);
        if (packet.getName().equals(PacketName.PLAYER_POSITION_AND_LOOK_CLIENTBOUND)) {
            onPlayerPositionAndLook((PlayerPositionAndLookClientboundPacket) packet);
        }
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        x = packet.getX();
        feetY = packet.getFeetY();
        z = packet.getZ();
        yaw = packet.getYaw();
        pitch = packet.getPitch();
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();
        checkMove();
    }

    private void checkMove() {
        boolean reachedDestination = reachedDestination();
        boolean looksWhereShould = looksWhereShould();

        if (!isMoving()) {
            return;
        }

        if (!reachedDestination) {
            changePosition();
        }

        if (!looksWhereShould) {
            changeLook();
        }

        if (reachedDestination && !looksWhereShould) {
            connection.sendPacket(new PlayerLookPacket(yaw, pitch, true));
        } else if (!reachedDestination && !looksWhereShould) {
            connection.sendPacket(PlayerPositionAndLookServerboundPacket.builder()
                    .x(x)
                    .feetY(feetY)
                    .z(z)
                    .yaw(yaw)
                    .pitch(pitch)
                    .onGround(true)
                    .build()
            );
        } else {
            connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
        }
    }

    private void changeLook() {
        yaw = move.getYaw().get();
        pitch = move.getPitch().get();
    }

    private void changePosition() {
        if (Math.abs(move.getDestinationX() - x) > Math.abs(move.getXMove())) {
            x += move.getXMove();
        } else {
            x = move.getDestinationX();
        }

        if (Math.abs(move.getDestinationZ() - z) > Math.abs(move.getZMove())) {
            z += move.getZMove();
        } else {
            z = move.getDestinationZ();
        }
    }

    private boolean looksWhereShould() {
        return move == null || move.getYaw().isEmpty() || (yaw == move.getYaw().get() && pitch == move.getPitch().get());
    }

    private boolean reachedDestination() {
        return move == null || (x == move.getDestinationX() && z == move.getDestinationZ());
    }
}
