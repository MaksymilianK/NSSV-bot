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

import static java.lang.Math.PI;
import static java.lang.Math.atan2;

public abstract class MovableSession extends Session {

    public static final double MAX_MOVE = 0.18d;
    public static final double MAX_LOOK = 25.0d;

    protected HorizontalMove move;

    public MovableSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
        timer.setTimeToNow("nextUpdate");
        timer.setTimeToNow("nextPossibleMove");
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

    protected void checkUpdate() {
        if (timer.isNowAfter("nextUpdate")) {
            delayNextUpdate();
            connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
        }
    }

    protected void delayNextUpdate() {
        timer.setTimeFromNow("nextUpdate", Duration.ofSeconds(1));
    }

    @Override
    protected void onEveryCheck() {
        super.onEveryCheck();
        if (!status.equals(Status.GAME)) {
            return;
        }

        checkMove();
        checkUpdate();
    }

    @Override
    protected void onPlayerPositionAndLook(PlayerPositionAndLookClientboundPacket packet) {
        super.onPlayerPositionAndLook(packet);
        if (!status.equals(Status.GAME)) {
            return;
        }

        if (packet.getFlags() == (byte) 0) {
            connection.sendPacket(PlayerPositionAndLookServerboundPacket.builder()
                    .x(x)
                    .feetY(packet.getFeetY())
                    .z(z)
                    .yaw(packet.getFlags() == (byte) 0 ? packet.getYaw() : yaw)
                    .pitch(packet.getFlags() == (byte) 0 ? packet.getPitch() : pitch)
                    .onGround(false)
                    .build()
            );
            connection.sendPacket(new PlayerPacket(true));
        } else {
            connection.sendPacket(PlayerPositionAndLookServerboundPacket.builder()
                    .x(x)
                    .feetY(packet.getFeetY())
                    .z(z)
                    .yaw(yaw)
                    .pitch(pitch)
                    .onGround(true)
                    .build()
            );
        }

        timer.setTimeFromNow("nextPossibleMove", Duration.ofSeconds(2));
        timer.setTimeFromNow("nextUpdate", Duration.ofSeconds(1));

        System.out.println("received " + packet.getX() + " " + packet.getFeetY() + " " + packet.getZ() + " " + packet.getYaw() + " " + packet.getPitch());
        System.out.println("sent cpl " + x + " " + feetY + " " + z + " " + yaw + " " + pitch);
    }

    protected float getYaw(double x0, double z0, float x, float z) {
        double dx = x - x0;
        double dz = z - z0;

        return (float) (-atan2(dx,dz) / PI * 180.0);
    }

    protected float getPitch(double x0, double z0, float x, float y, float z) {
        double dx = x + 0.5f - x0;
        double dy = (float) (y + 0.5f - feetY - 1.62f);
        double dz = z + 0.5f - z0;
        float r = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        return (float) (-Math.asin(dy/r) / PI * 180);
    }

    private void checkMove() {
        if (move == null || !timer.isNowAfter("nextPossibleMove")) {
            return;
        }

        boolean reachedDestination = reachedDestination();
        boolean looksWhereShould = looksWhereShould();

        if (reachedDestination && !looksWhereShould) {
            changeLook();
            connection.sendPacket(new PlayerLookPacket(yaw, pitch, true));
            //System.out.println("cl " + yaw + " " + pitch);
        } else if (!reachedDestination && looksWhereShould) {
            changePosition();
            connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
            //System.out.println("cp " + x + " " + feetY + " " + z);
        } else if (!reachedDestination) {
            changePosition();
            //connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));
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
            //System.out.println("cpl " + x + " " + feetY + " " + z + " " + yaw + " " + pitch);
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
        return move == null || (Math.abs(move.getYaw().get() - yaw) < 0.03f && Math.abs(move.getPitch().get() - pitch) < 0.03f);
    }

    private boolean reachedDestination() {
        return move == null || (Math.abs(move.getDestinationX() - x) < 0.03 && Math.abs(move.getDestinationZ() - z) < 0.03);
    }
}
