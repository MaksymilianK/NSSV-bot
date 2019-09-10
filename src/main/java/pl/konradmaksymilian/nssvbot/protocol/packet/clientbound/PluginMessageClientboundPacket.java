package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class PluginMessageClientboundPacket implements Packet {

    private final String channel;
    private final String data;
    
    public PluginMessageClientboundPacket(String channel, String data) {
        this.channel = channel;
        this.data = data;
    }

    public String getChannel() {
        return channel;
    }
    
    public String getData() {
        return data;
    }

    @Override
    public PacketName getName() {
        return PacketName.PLUGIN_MESSAGE_CLIENTBOUND;
    }
    
    @Override
    public String toString() {
        return getName() + " - channel: " + channel + ", data: " + data;
    }
}
