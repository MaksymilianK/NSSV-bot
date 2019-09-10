package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.PluginMessageServerboundPacket;

public final class RegisterPacket extends PluginMessageServerboundPacket {

    private final String[] names;
    
    public RegisterPacket(String[] names) {
        super("REGISTER");
        this.names = names;
    }
    
    public String[] getNames() {
        return names;
    }
}
