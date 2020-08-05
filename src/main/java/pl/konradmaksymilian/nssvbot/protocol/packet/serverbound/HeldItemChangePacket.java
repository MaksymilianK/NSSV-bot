package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class HeldItemChangePacket implements Packet {

    private final short slot;

    public HeldItemChangePacket(short slot) {
        this.slot = slot;
    }

    public short getSlot() {
        return slot;
    }

    @Override
    public PacketName getName() {
        return PacketName.HELD_ITEM_CHANGE;
    }
}
