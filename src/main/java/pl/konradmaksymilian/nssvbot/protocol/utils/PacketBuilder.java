package pl.konradmaksymilian.nssvbot.protocol.utils;

import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import pl.konradmaksymilian.nssvbot.protocol.packet.DisconnectLoginPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.LoginSuccessPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.SetCompressionPacket;

public class PacketBuilder {
    
    public static DisconnectLoginPacket disconnectLogin(DataInputStream in) throws IOException {
        return new DisconnectLoginPacket(StringConverter.readString(in));
    }
    
    public static SetCompressionPacket setCompression(DataInputStream in) throws IOException {
        return new SetCompressionPacket(VarIntLongConverter.readVarInt(in).getValue());
    }
    
    public static LoginSuccessPacket loginSuccess(DataInputStream in) throws IOException {
        return new LoginSuccessPacket(StringConverter.readString(in), StringConverter.readString(in));
    }
}
