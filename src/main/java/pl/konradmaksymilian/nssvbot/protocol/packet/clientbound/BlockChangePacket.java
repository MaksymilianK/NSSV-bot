package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class BlockChangePacket implements Packet {

    private final Position position;
    private final int stateID;

    public BlockChangePacket(Position position, int stateID) {
        this.position = position;
        this.stateID = stateID;
    }

    public Position getPosition() {
        return position;
    }

    public int getStateID() {
        return stateID;
    }

    @Override
    public PacketName getName() {
        return PacketName.BLOCK_CHANGE;
    }
}
