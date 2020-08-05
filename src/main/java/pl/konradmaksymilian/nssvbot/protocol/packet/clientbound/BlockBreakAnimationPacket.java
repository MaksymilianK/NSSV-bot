package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class BlockBreakAnimationPacket implements Packet {

    private final Position location;
    private final byte destroyStage;

    public BlockBreakAnimationPacket(Position location, byte destroyStage) {
        this.location = location;
        this.destroyStage = destroyStage;
    }

    public Position getLocation() {
        return location;
    }

    public byte getDestroyStage() {
        return destroyStage;
    }

    @Override
    public PacketName getName() {
        return PacketName.BLOCK_BREAK_ANIMATION;
    }
}
