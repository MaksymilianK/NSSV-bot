package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class WindowItemsPacket implements Packet {

    private final int windowId;
    private final byte[][] slotData;

    public WindowItemsPacket(int windowId, byte[][] slotData) {
        this.windowId = windowId;
        this.slotData = slotData;
    }

    public int getWindowId() {
        return windowId;
    }

    public byte[][] getSlotData() {
        return slotData;
    }

    @Override
    public PacketName getName() {
        return PacketName.WINDOW_ITEMS;
    }
}
