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
    private HorizontalMove move;

    public MovableSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
    }

    protected void setNewDestination(double newX, double newZ) {
        this.move = HorizontalMove.builder()
                .currentX(x)
                .currentZ(z)
                .destinationX(newX)
                .destinationZ(newZ)
                .build();
    }

    protected void setNewDestination(double newX, double newZ, float yaw, float pitch) {
        this.move = HorizontalMove.builder()
                .currentX(x)
                .currentZ(z)
                .destinationX(newX)
                .destinationZ(newZ)
                .yaw(yaw)
                .pitch(pitch)
                .build();
    }

    public boolean isMoving() {
        return move != null;
    }

    @Override
    protected void setUpTimer() {
        super.setUpTimer();
        timer.setDuration("afkMove", Duration.ofSeconds(30));
        timer.setTimeFromNow("nextAfkMove", timer.getDuration("afkMove"));
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
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();
        if (move != null) {
            move();
        }
    }

    private void move() {
        if (changePosition()) {
            if (move.getYaw().isPresent()) {
                connection.sendPacket(PlayerPositionAndLookServerboundPacket.builder()
                        .x(x)
                        .feetY(feetY)
                        .z(z)
                        .yaw(move.getYaw().get())
                        .pitch(move.getPitch().get())
                        .onGround(true)
                        .build()
                );
            } else {
                connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
            }
            move = null;
        } else {
            connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
        }
    }

    /**
     * @return {@code true} if that was the last step; {@code false} otherwise
     */
    private boolean changePosition() {
        boolean xMove = true;
        if (Math.abs(move.getDestinationX() - x) > Math.abs(move.getXMove())) {
            x += move.getXMove();
        } else {
            x = move.getDestinationX();
            xMove = false;
        }

        if (Math.abs(move.getDestinationZ() - z) > Math.abs(move.getZMove())) {
            z += move.getZMove();
        } else {
            z = move.getDestinationZ();
            if (!xMove) {
                return true;
            }
        }
        return false;
    }
}
