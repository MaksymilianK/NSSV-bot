package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class JoinGamePacket implements Packet {

    private final int playerEid;

    public JoinGamePacket(int playerEid) {
        this.playerEid = playerEid;
    }

    public int getPlayerEid() {
        return playerEid;
    }

    @Override
    public PacketName getName() {
        return PacketName.JOIN_GAME;
    }
    
    @Override
    public String toString() {
        return getName().toString();
    }
}
