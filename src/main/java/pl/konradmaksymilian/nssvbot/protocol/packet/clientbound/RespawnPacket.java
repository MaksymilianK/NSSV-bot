package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class RespawnPacket implements Packet {

    private final int dimension;
    private final int difficulty;
    private final int gamemode;
    private final String levelType;
    
    public RespawnPacket(int dimension, int difficulty, int gamemode, String levelType) {
        this.dimension = dimension;
        this.difficulty = difficulty;
        this.gamemode = gamemode;
        this.levelType = levelType;
    }
    
    public int getDimension() {
        return dimension;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public int getGamemode() {
        return gamemode;
    }
    
    public String getLevelType() {
        return levelType;
    }

    @Override
    public PacketName getName() {
        return PacketName.RESPAWN;
    }
    
    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(getName());
        builder.append(" - dimension: ");
        builder.append(dimension);
        builder.append(", difficulty: ");
        builder.append(difficulty);
        builder.append(", gamemode: ");
        builder.append(gamemode);
        builder.append(", level type: ");
        builder.append(levelType);
        return builder.toString();
    }
}
