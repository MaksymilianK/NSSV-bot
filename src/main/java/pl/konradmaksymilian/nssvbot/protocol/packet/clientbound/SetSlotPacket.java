package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class SetSlotPacket implements Packet {

    private final int windowId;
    private final int slot;
    private final byte[] slotData;

    public SetSlotPacket(int windowId, int slot, byte[] slotData) {
        this.windowId = windowId;
        this.slot = slot;
        this.slotData = slotData;
    }

    public int getWindowId() {
        return windowId;
    }

    public int getSlot() {
        return slot;
    }

    public byte[] getSlotData() {
        return slotData;
    }

    @Override
    public PacketName getName() {
        return PacketName.SET_SLOT;
    }
}
