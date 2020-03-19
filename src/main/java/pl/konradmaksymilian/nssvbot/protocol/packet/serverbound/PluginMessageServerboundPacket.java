package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public abstract class PluginMessageServerboundPacket implements Packet {

    private final String channel;
    
    public PluginMessageServerboundPacket(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public PacketName getName() {
        return PacketName.PLUGIN_MESSAGE_SERVERBOUND;
    }
}
