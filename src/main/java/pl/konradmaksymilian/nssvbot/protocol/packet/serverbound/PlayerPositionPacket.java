package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class PlayerPositionPacket implements Packet {

    private final double x;
    private final double feetY;
    private final double z;
    private final boolean onGround;

    public PlayerPositionPacket(double x, double feetY, double z, boolean onGround) {
        this.x = x;
        this.feetY = feetY;
        this.z = z;
        this.onGround = onGround;
    }

    public double getX() {
        return x;
    }

    public double getFeetY() {
        return feetY;
    }

    public double getZ() {
        return z;
    }

    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public PacketName getName() {
        return PacketName.PLAYER_POSITION;
    }
}
