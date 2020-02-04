package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.KeepAlivePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class KeepAliveServerboundPacket extends KeepAlivePacket {
    
    public KeepAliveServerboundPacket(long keepAliveId) {
        super(keepAliveId);
    }

    @Override
    public PacketName getName() {
        return PacketName.KEEP_ALIVE_SERVERBOUND;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof KeepAliveServerboundPacket) {
            return getKeepAliveId() == ((KeepAliveServerboundPacket) obj).getKeepAliveId();
        } else {
            return false;
        }
    }
}
