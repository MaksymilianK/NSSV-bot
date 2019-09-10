package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class TeleportConfirmPacket implements Packet {

    private final int teleportId;
    
    public TeleportConfirmPacket(int teleportId) {
        this.teleportId = teleportId;
    }
    
    public int getTeleportId() {
        return teleportId;
    }

    @Override
    public PacketName getName() {
        return PacketName.TELEPORT_CONFIRM;
    }
}
