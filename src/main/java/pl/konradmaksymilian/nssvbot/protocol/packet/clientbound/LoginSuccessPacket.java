package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class LoginSuccessPacket implements Packet {

    private final String UUID;
    private final String username;
    
    public LoginSuccessPacket(String UUID, String username) {
        this.UUID = UUID;
        this.username = username;
    }
    
    public String getUUID() {
        return UUID;
    }
    
    public String getUsername() {
        return username;
    }

    @Override
    public PacketName getName() {
        return PacketName.LOGIN_SUCCESS;
    }
    
    @Override
    public String toString() {
        return getName() + " - username: " + username;
    }
}
