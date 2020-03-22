package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class PlayerDiggingPacket implements Packet {

    private final int status;
    private final Position location;
    private final int face;

    public PlayerDiggingPacket(int status, Position location, int face) {
        this.status = status;
        this.location = location;
        this.face = face;
    }

    public int getStatus() {
        return status;
    }

    public Position getLocation() {
        return location;
    }

    public int getFace() {
        return face;
    }

    @Override
    public PacketName getName() {
        return PacketName.PLAYER_DIGGING;
    }
}
