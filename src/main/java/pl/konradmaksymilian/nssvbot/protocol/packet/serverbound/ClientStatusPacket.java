package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public class ClientStatusPacket implements Packet {

    @Override
    public PacketName getName() {
        return PacketName.CLIENT_STATUS;
    }
}
