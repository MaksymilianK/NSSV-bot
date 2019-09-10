package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.PluginMessageServerboundPacket;

public final class KickPlayerPacket extends PluginMessageServerboundPacket {

    private final String nick;
    private final String reason;
    
    public KickPlayerPacket(String channel, String nick, String reason) {
        super(channel);
        this.nick = nick;
        this.reason = reason;
    }
    
    public String getNick() {
        return nick;
    }
    
    public String getReason() {
        return reason;
    }
}
