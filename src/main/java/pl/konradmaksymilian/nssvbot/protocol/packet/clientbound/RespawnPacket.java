package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class RespawnPacket implements Packet {

    private final int dimension;
    private final int gamemode;
    
    public RespawnPacket(int dimension, int gamemode) {
        this.dimension = dimension;
        this.gamemode = gamemode;
    }
    
    public int getDimension() {
        return dimension;
    }

    public int getGamemode() {
        return gamemode;
    }
    
    @Override
    public PacketName getName() {
        return PacketName.RESPAWN;
    }
    
    @Override
    public String toString() {
        return getName() + " - dimension: " + dimension + ", gamemode: " + gamemode;
    }
}
