package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChangeGameStatePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectLoginPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectPlayPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.JoinGamePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.KeepAliveClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.LoginSuccessPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PluginMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.RespawnPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.SetCompressionPacket;

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
        in.readInt();
        in.readByte();
        in.readInt();
        in.readByte();
        in.readByte();
        StringConverter.readString(in).getValue();
        in.readBoolean();
        return new JoinGamePacket();
    }

    public static ChatMessageClientboundPacket chatMessageClientbound(DataInputStream in) throws IOException {
        return new ChatMessageClientboundPacket(ChatConverter.convert(StringConverter.readString(in).getValue()));
    }

    public static KeepAliveClientboundPacket keepAliveClientbound(DataInputStream in) throws IOException {
        return new KeepAliveClientboundPacket(in.readLong());
    }

    public static Packet playerPositionAndLook(DataInputStream in) throws IOException {
        in.readDouble();
        in.readDouble();
        in.readDouble();
        in.readFloat();
        in.readFloat();
        in.readByte();
        return new PlayerPositionAndLookPacket(VarIntLongConverter.readVarInt(in).getValue());
    }

    public static RespawnPacket respawn(DataInputStream in) throws IOException {
        int dimension = in.readInt();
        in.readByte();
        int gamemode = in.readByte();
        StringConverter.readString(in).getValue();
        return new RespawnPacket(dimension, gamemode);
    }
}
