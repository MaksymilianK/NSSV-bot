package pl.konradmaksymilian.nssvbot.protocol.packet;


public final class LoginStartPacket implements Packet {

    private final String username;
    
    public LoginStartPacket(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }

    @Override
    public PacketName getName() {
        return PacketName.LOGIN_START;
    }
}
