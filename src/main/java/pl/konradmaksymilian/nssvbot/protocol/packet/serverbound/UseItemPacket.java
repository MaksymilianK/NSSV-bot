package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class UseItemPacket implements Packet {

    private final int hand;

    public UseItemPacket(int hand) {
        this.hand = hand;
    }

    public int getHand() {
        return hand;
    }

    @Override
    public PacketName getName() {
        return PacketName.USE_ITEM;
    }
}
