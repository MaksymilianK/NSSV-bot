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
    protected HorizontalMove move;

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

    protected float getYaw(double x0, double z0, float x, float z) {
        double dx = x + 0.5f - x0;
        double dz = z + 0.5f - z0;

        if (dx < 0) {
            return (float) ((Math.atan(dz / dx) + Math.PI * 5f / 2f) / Math.PI * 180);

        } else {
            return (float) ((Math.atan(dz / dx) + Math.PI * 3f / 2f) / Math.PI * 180);
        }
    }

    protected float getPitch(double x0, double z0, float x, float y, float z) {
        double dx = x + 0.5f - x0;
        double dy = (float) (y + 0.5f - feetY - 1.62f);
        double dz = z + 0.5f - z0;
        float r = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        return (float) (-Math.asin(dy/r) / Math.PI * 180);
    }

    private void checkMove() {
        boolean reachedDestination = reachedDestination();
        boolean looksWhereShould = looksWhereShould();

        if (!reachedDestination) {
            changePosition();
        }

        if (!looksWhereShould) {
            changeLook();
        }

        if (!reachedDestination || !looksWhereShould) {
            connection.sendPacket(PlayerPositionAndLookServerboundPacket.builder()
                    .x(x)
                    .feetY(feetY)
                    .z(z)
                    .yaw(yaw)
                    .pitch(pitch)
                    .onGround(true)
                    .build()
            );
        }

       /* if (reachedDestination && !looksWhereShould) {
            changeLook();
            connection.sendPacket(new PlayerLookPacket(yaw, pitch, true));
        } else if (!reachedDestination && looksWhereShould) {
            changePosition();
            connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
        } else if (!reachedDestination) {
            changePosition();
            changeLook();
            connection.sendPacket(PlayerPositionAndLookServerboundPacket.builder()
                    .x(x)
                    .feetY(feetY)
                    .z(z)
                    .yaw(yaw)
                    .pitch(pitch)
                    .onGround(true)
                    .build()
            );
        } */
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
