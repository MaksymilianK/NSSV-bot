package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ChangeGameStatePacket implements Packet {

    private final int reason;
    private final float value;
    
    public ChangeGameStatePacket(int reason, float value) {
        this.reason = reason;
        this.value = value;
    }

    public int getReason() {
        return reason;
    }
    
    public float getValue() {
        return value;
    }
    
    @Override
    public PacketName getName() {
        return PacketName.CHANGE_GAME_STATE;
    }
    
    @Override
    public String toString() {
        return getName() + " - reason: " + reason + ", value: " + value;
    }
}
