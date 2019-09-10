package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ChatMessageServerboundPacket implements Packet {

    private final String message;
    
    public ChatMessageServerboundPacket(String message) {
        if (message.length() > 256) {
            throw new IllegalArgumentException("Chat message cannot be longer than 256 characters");
        }
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }

    @Override
    public PacketName getName() {
        return PacketName.CHAT_MESSAGE_SERVERBOUND;
    }
}
