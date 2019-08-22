package pl.konradmaksymilian.nssvbot.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import pl.konradmaksymilian.nssvbot.connection.ConnectionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.LoginStartPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
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
            }
            
            writePacket(byteBuffer);
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
}
