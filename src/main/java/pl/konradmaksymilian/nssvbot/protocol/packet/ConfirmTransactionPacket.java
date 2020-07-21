package pl.konradmaksymilian.nssvbot.protocol.packet;

public abstract class ConfirmTransactionPacket implements Packet {

    private final int windowId;
    private final int actionNumber;
    private final boolean accepted;

    public ConfirmTransactionPacket(int windowId, int actionNumber, boolean accepted) {
        this.windowId = windowId;
        this.actionNumber = actionNumber;
        this.accepted = accepted;
    }

    public int getWindowId() {
        return windowId;
    }

    public int getActionNumber() {
        return actionNumber;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
