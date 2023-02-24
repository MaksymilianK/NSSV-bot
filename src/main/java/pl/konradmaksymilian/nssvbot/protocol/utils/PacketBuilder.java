package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.nullicorn.nedit.NBTInputStream;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.NBTWriter;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HeldItemChangeServerboundPacket;

public class PacketBuilder {

    public static DisconnectLoginPacket disconnectLogin(DataInputStream in) throws IOException {
        return new DisconnectLoginPacket(ChatConverter.convert(StringConverter.readString(in).getValue()));
    }
    
    public static DisconnectPlayPacket disconnectPlay(DataInputStream in) throws IOException {
        return new DisconnectPlayPacket(ChatConverter.convert(StringConverter.readString(in).getValue()));
    }
    
    public static SetCompressionPacket setCompression(DataInputStream in) throws IOException {
        return new SetCompressionPacket(VarIntLongConverter.readVarInt(in).getValue());
    }
    
    public static LoginSuccessPacket loginSuccess(DataInputStream in) throws IOException {
        return new LoginSuccessPacket(StringConverter.readString(in).getValue(), StringConverter.readString(in).getValue());
    }
    
    public static JoinGamePacket joinGame(DataInputStream in) throws IOException {
        int playerEid = in.readInt();
        in.readByte();
        in.readInt();
        in.readByte();
        in.readByte();
        StringConverter.readString(in).getValue();
        in.readBoolean();
        return new JoinGamePacket(playerEid);
    }

    public static ChatMessageClientboundPacket chatMessageClientbound(DataInputStream in) throws IOException {
        return new ChatMessageClientboundPacket(ChatConverter.convert(StringConverter.readString(in).getValue()));
    }

    public static KeepAliveClientboundPacket keepAliveClientbound(DataInputStream in) throws IOException {
        return new KeepAliveClientboundPacket(in.readLong());
    }

    public static Packet playerPositionAndLook(DataInputStream in) throws IOException {
        return PlayerPositionAndLookClientboundPacket.builder()
                .x(in.readDouble())
                .feetY(in.readDouble())
                .z(in.readDouble())
                .yaw(in.readFloat())
                .pitch(in.readFloat())
                .flags(in.readByte())
                .teleportId(VarIntLongConverter.readVarInt(in).getValue())
                .build();
    }

    public static RespawnPacket respawn(DataInputStream in) throws IOException {
        int dimension = in.readInt();
        in.readByte();
        int gamemode = in.readByte();
        StringConverter.readString(in).getValue();
        return new RespawnPacket(dimension, gamemode);
    }

    public static OpenWindowPacket openWindow(DataInputStream in) throws IOException {
        return new OpenWindowPacket(in.readUnsignedByte(), StringConverter.readString(in).getValue(),
                ChatConverter.convert(StringConverter.readString(in).getValue()), in.readUnsignedByte());
    }

    public static ConfirmTransactionClientboundPacket confirmTransactionClientbound(DataInputStream in) throws IOException {
        return new ConfirmTransactionClientboundPacket(in.readByte(), in.readShort(), in.readBoolean());
    }

    public static SetSlotPacket setSlot(DataInputStream in) throws IOException {
        int windowId = in.readByte();
        int slotId = in.readShort();
        return new SetSlotPacket(windowId, slotId, in.readAllBytes());
    }

    public static HeldItemChangeServerboundPacket heldItemChange(DataInputStream in) throws IOException {
        return new HeldItemChangeServerboundPacket(in.readByte());
    }

    public static BlockBreakAnimationPacket blockBreakAnimation(DataInputStream in) throws IOException {
        VarIntLongConverter.readVarInt(in);
        return new BlockBreakAnimationPacket(PositionConverter.read(in), in.readByte());
    }

    public static BlockChangePacket blockChange(DataInputStream in) throws IOException {
        return new BlockChangePacket(PositionConverter.read(in), VarIntLongConverter.readVarInt(in).getValue());
    }

    private void readNbt(DataInputStream in, List<Byte> bytes) throws IOException {
        byte one = in.readByte();
        byte two = in.readByte();
        bytes.add(one);
        bytes.add(two);
    }

    public static WindowItemsPacket windowItems(DataInputStream in) throws IOException {
        int windowId = in.readUnsignedByte();
        int len = in.readShort();
        byte[][] slotData = new byte[len][];

        byte[] copy = in.readAllBytes();
        var inB = new ByteArrayInputStream(copy);
        for (int i = 0; i < slotData.length; i++) {
            var bytes = new ArrayList<Byte>();

            bytes.add(inB.readNBytes(1)[0]);
            bytes.add(inB.readNBytes(1)[0]);

            if (bytes.get(0) != -1 || bytes.get(1) != -1) {
                bytes.add(inB.readNBytes(1)[0]);

                bytes.add(inB.readNBytes(1)[0]);
                bytes.add(inB.readNBytes(1)[0]);

                if (copy[5] != 0) {
                    var inNbt = new NBTInputStream(inB);
                    var c = inNbt.readFully();

                    var out = new ByteArrayOutputStream();
                    NBTWriter.write(c, out);
                    byte[] by = out.toByteArray();

                    for (int j = 0; j < by.length; j++) {
                        bytes.add(by[j]);
                    }
                } else {
                    bytes.add(inB.readNBytes(1)[0]);
                }
            }

            slotData[i] = new byte[bytes.size()];
            for (int j = 0; j < bytes.size(); j++) {
                slotData[i][j] = bytes.get(j);
            }
            copy = inB.readAllBytes();
            inB = new ByteArrayInputStream(copy);
        }

        return new WindowItemsPacket(windowId, slotData);
    }
}
