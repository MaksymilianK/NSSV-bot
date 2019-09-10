package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ChatMessageClientboundPacket implements Packet {
    
    private final ChatMessage message;
    
    public ChatMessageClientboundPacket(ChatMessage message) {
        this.message = message;
    }
    
    public ChatMessage getMessage() {
        return message;
    }

    @Override
    public PacketName getName() {
        return PacketName.CHAT_MESSAGE_CLIENTBOUND;
    }
    
    @Override
    public String toString() {
        return getName() + " - message: " + message;
    }
}