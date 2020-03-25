package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.DataInputStream;
import java.io.IOException;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.*;

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
        var builder = PlayerPositionAndLookClientboundPacket.builder()
                .x(in.readDouble())
                .feetY(in.readDouble())
                .z(in.readDouble())
                .yaw(in.readFloat())
                .pitch(in.readFloat());
        in.readByte();
        return builder
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
}
