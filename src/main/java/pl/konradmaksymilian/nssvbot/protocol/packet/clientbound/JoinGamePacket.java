package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class JoinGamePacket implements Packet {

    private final int entityId;
    private final int gamemode;
    private final int dimension;
    private final int difficulty;
    private final int maxPlayers;
    private final String levelType;
    private final boolean reducedDebugInfo;
    
    public JoinGamePacket(int entityId, int gamemode, int dimension, int difficulty, int maxPlayers, String levelType,
            boolean reducedDebugInfo) {
        this.entityId = entityId;
        this.gamemode = gamemode;
        this.dimension = dimension;
        this.difficulty = difficulty;
        this.maxPlayers = maxPlayers;
        this.levelType = levelType;
        this.reducedDebugInfo = reducedDebugInfo;
    }
    
    public int getEntityId() {
        return entityId;
    }
    
    public int getGamemode() {
        return gamemode;
    }
    
    public int getDimension() {
        return dimension;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public String getLevelType() {
        return levelType;
    }
    
    public boolean isReducedDebugInfo() {
        return reducedDebugInfo;
    }

    @Override
    public PacketName getName() {
        return PacketName.JOIN_GAME;
    }
    
    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(getName());
        builder.append(" - entityId: ");
        builder.append(entityId);
        builder.append(", gamemode: ");
        builder.append(gamemode);
        builder.append(", dimension: ");
        builder.append(dimension);
        builder.append(", difficulty: ");
        builder.append(difficulty);
        builder.append(", maxPlayers: ");
        builder.append(maxPlayers);
        builder.append(", levelType: ");
        builder.append(levelType);
        builder.append(", reducedDebugInfo: ");
        builder.append(reducedDebugInfo);
        return builder.toString();
    }
}
