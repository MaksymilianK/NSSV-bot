package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class CloseWindowPacket implements Packet {

    private final int windowId;

    public CloseWindowPacket(int windowId) {
        this.windowId = windowId;
    }

    public int getWindowId() {
        return windowId;
    }

    @Override
    public PacketName getName() {
        return PacketName.CLOSE_WINDOW;
    }
}
