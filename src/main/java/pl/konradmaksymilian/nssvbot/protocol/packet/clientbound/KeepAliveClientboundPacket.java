package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.KeepAlivePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class KeepAliveClientboundPacket extends KeepAlivePacket {

    public KeepAliveClientboundPacket(long keepAliveId) {
        super(keepAliveId);
    }

    @Override
    public PacketName getName() {
        return PacketName.KEEP_ALIVE_CLIENTBOUND;
    }
}
