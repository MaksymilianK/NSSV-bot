package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

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
