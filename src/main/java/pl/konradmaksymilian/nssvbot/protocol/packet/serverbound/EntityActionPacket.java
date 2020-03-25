package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class EntityActionPacket implements Packet {

    private final int entityId;
    private final int actionId;

    public EntityActionPacket(int entityId, int actionId) {
        this.entityId = entityId;
        this.actionId = actionId;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getActionId() {
        return actionId;
    }

    @Override
    public PacketName getName() {
        return PacketName.ENTITY_ACTION;
    }
}
