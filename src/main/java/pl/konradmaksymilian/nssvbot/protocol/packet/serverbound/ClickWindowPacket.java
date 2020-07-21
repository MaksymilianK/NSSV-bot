package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ClickWindowPacket implements Packet {

    private final int windowId;
    private final int slot;
    private final int button;
    private final int actionNumber;
    private final int mode;
    private final byte[] slotData;

    public ClickWindowPacket(int windowId, int slot, int button, int actionNumber, int mode, byte[] slotData) {
        this.windowId = windowId;
        this.slot = slot;
        this.button = button;
        this.actionNumber = actionNumber;
        this.mode = mode;
        this.slotData = slotData;
    }

    public int getWindowId() {
        return windowId;
    }

    public int getSlot() {
        return slot;
    }

    public int getButton() {
        return button;
    }

    public int getActionNumber() {
        return actionNumber;
    }

    public int getMode() {
        return mode;
    }

    public byte[] getSlotData() {
        return slotData;
    }

    @Override
    public PacketName getName() {
        return PacketName.CLICK_WINDOW;
    }
}
