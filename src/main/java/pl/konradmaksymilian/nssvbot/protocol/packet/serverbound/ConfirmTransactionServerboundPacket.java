package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.ConfirmTransactionPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ConfirmTransactionServerboundPacket extends ConfirmTransactionPacket {

    public ConfirmTransactionServerboundPacket(int windowId, int actionNumber, boolean accepted) {
        super(windowId, actionNumber, accepted);
    }

    @Override
    public PacketName getName() {
        return PacketName.CONFIRM_TRANSACTION_SERVERBOUND;
    }
}
