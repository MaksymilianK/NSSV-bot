package pl.konradmaksymilian.nssvbot.protocol;

import java.util.ArrayList;
import java.util.List;

import pl.konradmaksymilian.nssvbot.protocol.ChatComponent;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.utils.ChatFormatter;

public final class ChatMessage {
    
    private final List<ChatComponent> components;
    
    private ChatMessage(List<ChatComponent> components) {
        this.components = components;
    }
    
    public List<ChatComponent> getComponents() {
        return components;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return ChatFormatter.getPureText(components);
    }

    public final static class Builder {
        
        private final List<ChatComponent> builder = new ArrayList<>();
        
        Builder() {}
        
        public Builder append(ChatComponent component) {
            builder.add(component);
            return this;
        }
        
        /**
         * Builds and clears the buffer list so that next {@link ChatMessageClientboundPacket} objects can be create from scratch 
         * using the same builder 
         * 
         * @return new {@link ChatMessageClientboundPacket} with unmodifiable content
         */
        public ChatMessage build() {
            var message = new ChatMessage(List.copyOf(builder));
            builder.clear();
            return message;
        }
    }
}
