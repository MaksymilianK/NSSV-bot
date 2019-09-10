package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;

public abstract class DisconnectPacket implements Packet {

    private final ChatMessage reason;

    public DisconnectPacket(ChatMessage reason) {
        this.reason = reason;
    }

    public ChatMessage getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return getName() + " - reason: " + reason;
    }
}
