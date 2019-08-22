package pl.konradmaksymilian.nssvbot.protocol.packet;

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
}
