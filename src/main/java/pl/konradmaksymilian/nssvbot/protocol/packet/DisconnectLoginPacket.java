package pl.konradmaksymilian.nssvbot.protocol.packet;


public final class DisconnectLoginPacket extends DisconnectPacket {

    public DisconnectLoginPacket(String reason) {
        super(reason);
    }
    
    @Override
    public PacketName getName() {
        return PacketName.DISCONNECT_LOGIN;
    }
}
