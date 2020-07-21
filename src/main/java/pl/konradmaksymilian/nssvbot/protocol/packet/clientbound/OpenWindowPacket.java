package pl.konradmaksymilian.nssvbot.protocol.packet.clientbound;

import pl.konradmaksymilian.nssvbot.protocol.ChatMessage;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class OpenWindowPacket implements Packet {

    private final int id;
    private final String type;
    private final ChatMessage title;
    private final int numberOfSlots;

    public OpenWindowPacket(int id, String type, ChatMessage title, int numberOfSlots) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.numberOfSlots = numberOfSlots;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public ChatMessage getTitle() {
        return title;
    }

    public int getNumberOfSlots() {
        return numberOfSlots;
    }

    @Override
    public PacketName getName() {
        return PacketName.OPEN_WINDOW;
    }
}
