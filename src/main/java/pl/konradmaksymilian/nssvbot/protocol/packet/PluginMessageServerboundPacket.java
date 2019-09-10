package pl.konradmaksymilian.nssvbot.protocol.packet;

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
