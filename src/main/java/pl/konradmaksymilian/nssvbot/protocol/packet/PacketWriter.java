package pl.konradmaksymilian.nssvbot.protocol.packet;

import java.io.*;

import pl.konradmaksymilian.nssvbot.connection.ConnectionException;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.Position;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.*;
import pl.konradmaksymilian.nssvbot.protocol.utils.PositionConverter;
import pl.konradmaksymilian.nssvbot.protocol.utils.StringConverter;
import pl.konradmaksymilian.nssvbot.protocol.utils.VarIntLongConverter;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class PacketWriter {
    
    private DataOutputStream out;
    private Compression compression;
    private ZlibCompressor zlib;
    
    public PacketWriter(ZlibCompressor zlib) {
        this.zlib = zlib;
    }
    
    public void write(Packet packet) {
        var byteBuffer = new ByteArrayOutputStream();
        try (var buffer = new DataOutputStream(byteBuffer)) {
            VarIntLongConverter.writeVarInt(packet.getName().getId(), buffer);
            writePacket(packet, buffer);
            writeData(byteBuffer);
        } catch (IOException e) {
            throw new ConnectionException("Error while writing a packet to the output stream", e);
        }
    }

    public void setOutput(DataOutputStream out) {
        this.out = out;
    }
    
    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    private void writePacket(Packet packet, DataOutputStream buffer) throws IOException {
        switch(packet.getName()) {
            case HANDSHAKE:
                writeHandshake((HandshakePacket) packet, buffer);
                break;
            case LOGIN_START:
                writeLoginStart((LoginStartPacket) packet, buffer);
                break;
            case KEEP_ALIVE_SERVERBOUND:
                writeKeepAliveServerbound((KeepAlivePacket) packet, buffer);
                break;
            case CHAT_MESSAGE_SERVERBOUND:
                writeChatMessageServerbound((ChatMessageServerboundPacket) packet, buffer);
                break;
            case TELEPORT_CONFIRM:
                writeTeleportConfirm((TeleportConfirmPacket) packet, buffer);
                break;
            case CLIENT_SETTINGS:
                writeClientSettings((ClientSettingsPacket) packet, buffer);
                break;
            case PLAYER_POSITION:
                writePlayerPosition((PlayerPositionPacket) packet, buffer);
                break;
            case PLAYER_LOOK:
                writePlayerLook((PlayerLookPacket) packet, buffer);
                break;
            case PLAYER_DIGGING:
                writePlayerDigging((PlayerDiggingPacket) packet, buffer);
                break;
            case PLAYER_BLOCK_PLACEMENT:
                writePlayerBlockPlacement((PlayerBlockPlacementPacket) packet, buffer);
                break;
            case PLAYER_POSITION_AND_LOOK_SERVERBOUND:
                writePlayerPositionAndLook((PlayerPositionAndLookServerboundPacket) packet, buffer);
                break;
            case ENTITY_ACTION:
                writeEntityAction((EntityActionPacket) packet, buffer);
                break;
            default:
                throw new UnrecognizedPacketException("Cannot write the packet '" + packet.getName() + "'");
        }
    }

    private void writePlayerPositionAndLook(PlayerPositionAndLookServerboundPacket packet,
                                            DataOutputStream buffer) throws IOException {
        buffer.writeDouble(packet.getX());
        buffer.writeDouble(packet.getFeetY());
        buffer.writeDouble(packet.getZ());
        buffer.writeFloat(packet.getYaw());
        buffer.writeFloat(packet.getPitch());
        buffer.writeBoolean(packet.isOnGround());
    }

    private void writeData(ByteArrayOutputStream buffer) throws IOException {
        var uncompressedLengthBuffer = new ByteArrayOutputStream();
        VarIntLongConverter.writeVarInt(buffer.size(), uncompressedLengthBuffer);
        
        if (compression.isCompressed()) {
            if (buffer.size() >= compression.getTreshold()) {
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
        byte[] compressedData = zlib.compress(buffer.toByteArray());
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

    private void writeHandshake(HandshakePacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(packet.getProtocolVersion(), buffer);
        StringConverter.writeString(packet.getServerAddress(), buffer);
        buffer.writeShort(packet.getServerPort());
        VarIntLongConverter.writeVarInt(packet.getNextState(), buffer);
    }
    
    private void writeLoginStart(LoginStartPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getUsername(), buffer);
    }
    
    private void writeKeepAliveServerbound(KeepAlivePacket packet, DataOutputStream buffer) throws IOException {
        buffer.writeLong(packet.getKeepAliveId());
    }
    
    private void writeChatMessageServerbound(ChatMessageServerboundPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getMessage(), buffer);
    }
    
    private void writeTeleportConfirm(TeleportConfirmPacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(packet.getTeleportId(), buffer);
    }
    
    private void writeClientSettings(ClientSettingsPacket packet, DataOutputStream buffer) throws IOException {
        StringConverter.writeString(packet.getLocale(), buffer);
        buffer.writeByte(packet.getViewDistance());
        VarIntLongConverter.writeVarInt(packet.getChatMode(), buffer);
        buffer.writeBoolean(packet.getChatColours());
        buffer.writeByte(packet.getDisplayedSkinParts());
        VarIntLongConverter.writeVarInt(packet.getMainHand(), buffer);
    }

    private void writePlayerPosition(PlayerPositionPacket packet, DataOutputStream buffer) throws IOException {
        buffer.writeDouble(packet.getX());
        buffer.writeDouble(packet.getFeetY());
        buffer.writeDouble(packet.getZ());
        buffer.writeBoolean(packet.isOnGround());
    }

    private void writePlayerLook(PlayerLookPacket packet, DataOutputStream buffer) throws IOException {
        buffer.writeFloat(packet.getYaw());
        buffer.writeFloat(packet.getPitch());
        buffer.writeBoolean(packet.isOnGround());
    }

    private void writePlayerDigging(PlayerDiggingPacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(packet.getStatus(), buffer);
        PositionConverter.write(packet.getLocation(), buffer);
        buffer.writeByte(packet.getFace());
    }

    private void writePlayerBlockPlacement(PlayerBlockPlacementPacket packet, DataOutputStream buffer) throws IOException {
        PositionConverter.write(packet.getLocation(), buffer);
        VarIntLongConverter.writeVarInt(packet.getFace(), buffer);
        VarIntLongConverter.writeVarInt(packet.getHand(), buffer);
        buffer.writeFloat(packet.getCursorX());
        buffer.writeFloat(packet.getCursorY());
        buffer.writeFloat(packet.getCursorZ());
    }

    private void writeEntityAction(EntityActionPacket packet, DataOutputStream buffer) throws IOException {
        VarIntLongConverter.writeVarInt(packet.getEntityId(), buffer);
        VarIntLongConverter.writeVarInt(packet.getActionId(), buffer);
        VarIntLongConverter.writeVarInt(0, buffer);
    }
}
