package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class DisconnectLoginPacket extends DisconnectPacket {

    public DisconnectLoginPacket(ChatMessage reason) {
        super(reason);
    }

    @Override
    public PacketName getName() {
        return PacketName.DISCONNECT_LOGIN;
    }
}
