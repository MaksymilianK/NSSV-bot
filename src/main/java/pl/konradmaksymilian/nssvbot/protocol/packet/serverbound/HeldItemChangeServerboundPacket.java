package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class HeldItemChangeServerboundPacket implements Packet {

    private final short slot;

    public HeldItemChangeServerboundPacket(short slot) {
        this.slot = slot;
    }

    public short getSlot() {
        return slot;
    }

    @Override
    public PacketName getName() {
        return PacketName.HELD_ITEM_CHANGE_SERVERBOUND;
    }
}
