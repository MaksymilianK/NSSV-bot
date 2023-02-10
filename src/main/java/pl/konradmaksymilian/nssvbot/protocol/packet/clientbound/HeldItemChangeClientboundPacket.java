package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class HeldItemChangeClientboundPacket implements Packet {

    private byte slot;

    public HeldItemChangeClientboundPacket(byte slot) {
        this.slot = slot;
    }

    public byte getSlot() {
        return slot;
    }

    public void setSlot(byte slot) {
        this.slot = slot;
    }

    @Override
    public PacketName getName() {
        return PacketName.HELD_ITEM_CHANGE_CLIENTBOUNT;
    }
}
