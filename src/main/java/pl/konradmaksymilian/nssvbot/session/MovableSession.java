package pl.konradmaksymilian.nssvbot.session;

import pl.konradmaksymilian.nssvbot.connection.ConnectionManager;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.PlayerPositionPacket;
import pl.konradmaksymilian.nssvbot.utils.Timer;

public abstract class MovableSession extends Session {

    public static final double MAX_MOVE = 0.25d;

    protected double x;
    protected double feetY;
    protected double z;
    private HorizontalMove move;
    private Runnable onMoveFinish;

    public MovableSession(ConnectionManager connection, Timer timer) {
        super(connection, timer);
    }

    protected void setNewDestination(double newX, double newZ) {
        onMoveFinish = null;
        move = new HorizontalMove(x, z, newX, newZ);
    }

    protected void setNewDestination(double newX, double newZ, Runnable onMoveFinish) {
        this.onMoveFinish = onMoveFinish;
        move = new HorizontalMove(x, z, newX, newZ);
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
                move = null;
            }
        }

        connection.sendPacket(new PlayerPositionPacket(x, feetY, z, true));

        if (move == null && onMoveFinish != null) {
            onMoveFinish.run();
        }
    }
}
