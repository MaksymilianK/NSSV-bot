package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class SetCompressionPacket implements Packet {

    private final int threshold;
    
    public SetCompressionPacket(int threshold) {
        this.threshold = threshold;
    }
    
    public int getThreshold() {
        return threshold;
    }

    @Override
    public PacketName getName() {
        return PacketName.SET_COMPRESSION;
    }
    
    @Override
    public String toString() {
        return getName() + " - threshold: " + threshold;
    }
}
