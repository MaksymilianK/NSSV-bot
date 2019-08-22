package pl.konradmaksymilian.nssvbot.protocol.packet;


public abstract class DisconnectPacket implements Packet {

    private final String reason;

    public DisconnectPacket(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
