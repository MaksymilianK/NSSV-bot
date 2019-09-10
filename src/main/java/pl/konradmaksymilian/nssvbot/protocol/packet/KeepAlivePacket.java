package pl.konradmaksymilian.nssvbot.protocol.packet;

public abstract class KeepAlivePacket implements Packet {

    private final long keepAliveId;

    public KeepAlivePacket(long keepAliveId) {
        this.keepAliveId = keepAliveId;
    }
    
    public long getKeepAliveId() {
        return keepAliveId;
    }
}
