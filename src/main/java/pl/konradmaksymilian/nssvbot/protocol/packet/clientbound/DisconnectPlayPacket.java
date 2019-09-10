package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class DisconnectPlayPacket extends DisconnectPacket {

    public DisconnectPlayPacket(ChatMessage reason) {
        super(reason);
    }

    @Override
    public PacketName getName() {
        return PacketName.DISCONNECT_PLAY;
    }
}
