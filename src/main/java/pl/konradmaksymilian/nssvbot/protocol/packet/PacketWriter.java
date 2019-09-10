package pl.konradmaksymilian.nssvbot.protocol.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import pl.konradmaksymilian.nssvbot.connection.ConnectionException;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ChatMessageServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ClientSettingsPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ClientStatusPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.KickPlayerPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.LoginStartPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.RegisterPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.TeleportConfirmPacket;
import pl.konradmaksymilian.nssvbot.protocol.utils.StringConverter;
import pl.konradmaksymilian.nssvbot.protocol.utils.VarIntLongConverter;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class PacketWriter {
    
    private DataOutputStream out;
    private Compression compression;
    private ZlibCompressor compressor = new ZlibCompressor();
    
    public void write(Packet packet) {
        var packetName = packet.getName();
        var byteBuffer = new ByteArrayOutputStream();
        try (var buffer = new DataOutputStream(byteBuffer)) {
            VarIntLongConverter.writeVarInt(packet.getName().getId(), buffer);

            if (packetName.equals(PacketName.HANDSHAKE)) {
                writeHandshakeData((HandshakePacket) packet, buffer);
            } else if (packetName.equals(PacketName.LOGIN_START)) {
                writeLoginStartData((LoginStartPacket) packet, buffer);
            } else if (packetName.equals(PacketName.KEEP_ALIVE_SERVERBOUND)) {
                writeKeepAliveServerboundData((KeepAlivePacket) packet, buffer);
            } else if (packetName.equals(PacketName.CHAT_MESSAGE_SERVERBOUND)) {
                writeChatMessageServerboundData((ChatMessageServerboundPacket) packet, buffer);
            } else if (packetName.equals(PacketName.PLUGIN_MESSAGE_SERVERBOUND)) {
                if (((PluginMessageServerboundPacket) packet).getChannel() == "REGISTER") {
                    writeRegisterData((RegisterPacket) packet, buffer);
                } else {
                writeKickPlayerData((KickPlayerPacket) packet, buffer);}
            } else if (packetName.equals(PacketName.CLIENT_STATUS)) {
                writeClientStatusData((ClientStatusPacket) packet, buffer);
            } else if (packetName.equals(PacketName.TELEPORT_CONFIRM)) {
                writeTeleportConfirmData((TeleportConfirmPacket) packet, buffer);
            } else if (packetName.equals(PacketName.CLIENT_SETTINGS)) {
                writeClientSettingsData((ClientSettingsPacket) packet, buffer);
            }
            
            writePacket(byteBuffer);
        } catch (IOException e) {
            throw new ConnectionException("Error while writing a packet to the output stream", e);
        }
    }
    
    private void writeRegisterData(RegisterPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getChannel(), buffer);
        StringConverter.writeString(packet.getNames()[0], buffer);
    }

    public void setOutput(DataOutputStream out) {
        this.out = out;
    }
    
    public void setCompression(Compression compression) {
        this.compression = compression;
    }
    
    private void writePacket(ByteArrayOutputStream buffer) throws IOException {
        var uncompressedLengthBuffer = new ByteArrayOutputStream();
        VarIntLongConverter.writeVarInt(buffer.size(), uncompressedLengthBuffer);
        
        if (compression.isCompressed()) {
            if (uncompressedLengthBuffer.size() >= compression.getTreshold()) {
                writeCompressedPacket(buffer, uncompressedLengthBuffer);
            } else {
                writeNotCompressedPacketInCompressedPacketFormat(buffer);
            }
        } else {
            writeNotCompressedPacket(buffer, uncompressedLengthBuffer);
        }
    }
    
    private void writeCompressedPacket(ByteArrayOutputStream buffer, ByteArrayOutputStream uncompressedLengthBuffer) 
            throws IOException {
        byte[] compressedData = compressor.compress(buffer.toByteArray());
        VarIntLongConverter.writeVarInt(uncompressedLengthBuffer.size() + compressedData.length, out);
        out.write(uncompressedLengthBuffer.toByteArray());
        out.write(compressedData);
    }

    private void writeNotCompressedPacketInCompressedPacketFormat(ByteArrayOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(1 + buffer.size(), out);
        VarIntLongConverter.writeVarInt(0, out);
        out.write(buffer.toByteArray());
    }
    
    private void writeNotCompressedPacket(ByteArrayOutputStream buffer, ByteArrayOutputStream uncompressedLengthBuffer) 
            throws IOException {
        out.write(uncompressedLengthBuffer.toByteArray());
        out.write(buffer.toByteArray());
    }

    private void writeHandshakeData(HandshakePacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(packet.getProtocolVersion(), buffer);
        StringConverter.writeString(packet.getServerAddress(), buffer);
        buffer.writeShort(packet.getServerPort());
        VarIntLongConverter.writeVarInt(packet.getNextState(), buffer);
    }
    
    private void writeLoginStartData(LoginStartPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getUsername(), buffer);
    }
    
    private void writeKeepAliveServerboundData(KeepAlivePacket packet, DataOutputStream buffer) throws IOException {
        buffer.writeLong(packet.getKeepAliveId());
    }
    
    private void writeChatMessageServerboundData(ChatMessageServerboundPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getMessage(), buffer);
    }
    
    private void writeKickPlayerData(KickPlayerPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getChannel(), buffer);
        writeString("KickPlayer", buffer);
        writeString(packet.getNick(), buffer);
        writeString(packet.getReason(), buffer);
        
        /* for (int i = 0; i < packet.getData().size(); i++) {
            StringConverter.writeString(packet.getData().get(i), buffer);
            if (i < packet.getData().size() - 1) {
                buffer.write(0x00);
            }
        }*/
    }
    
    private static void writeString(String s, DataOutputStream buffer) throws IOException {
        byte[] b = s.getBytes();
        buffer.writeShort(b.length);
        buffer.writeBytes(s);
    }
    
    private void writeClientStatusData(ClientStatusPacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(0, buffer);
    }
    
    private void writeTeleportConfirmData(TeleportConfirmPacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(packet.getTeleportId(), buffer);
    }
    
    private void writeClientSettingsData(ClientSettingsPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getLocale(), buffer);
        buffer.writeByte(packet.getViewDistance());
        VarIntLongConverter.writeVarInt(packet.getChatMode(), buffer);
        buffer.writeBoolean(packet.getChatColours());
        buffer.writeByte(255);

        // buffer.writeByte(packet.getDisplayedSkinParts());
        VarIntLongConverter.writeVarInt(packet.getMainHand(), buffer);
    }
}
