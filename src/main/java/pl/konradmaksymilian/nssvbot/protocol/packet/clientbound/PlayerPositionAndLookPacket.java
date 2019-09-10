package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class PlayerPositionAndLookPacket implements Packet {

    private final int teleportId;
    
    public PlayerPositionAndLookPacket(int teleportId) {
        this.teleportId = teleportId;
    }

    public int getTeleportId() {
        return teleportId;
    }
    
    @Override
    public PacketName getName() {
        return PacketName.PLAYER_POSITION_AND_LOOK;
    }
}
