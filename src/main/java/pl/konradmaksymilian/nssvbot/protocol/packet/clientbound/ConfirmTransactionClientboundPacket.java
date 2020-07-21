package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.packet.ConfirmTransactionPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class ConfirmTransactionClientboundPacket extends ConfirmTransactionPacket {

    public ConfirmTransactionClientboundPacket(int windowId, int actionNumber, boolean accepted) {
        super(windowId, actionNumber, accepted);
    }

    @Override
    public PacketName getName() {
        return PacketName.CONFIRM_TRANSACTION_CLIENTBOUND;
    }
}
