package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class AnimationPacket implements Packet {

    private final int hand;

    public AnimationPacket(int hand) {
        this.hand = hand;
    }

    public int getHand() {
        return hand;
    }

    @Override
    public PacketName getName() {
        return PacketName.ANIMATION;
    }
}
