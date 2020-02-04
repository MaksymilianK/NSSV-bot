package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class JoinGamePacket implements Packet {
    
    public JoinGamePacket() {}

    @Override
    public PacketName getName() {
        return PacketName.JOIN_GAME;
    }
    
    @Override
    public String toString() {
        return getName().toString();
    }
}
