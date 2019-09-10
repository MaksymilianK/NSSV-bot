package pl.konradmaksymilian.nssvbot.protocol.packet;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.DataFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.konradmaksymilian.nssvbot.connection.ConnectionException;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.ReadField;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.utils.PacketBuilder;
import pl.konradmaksymilian.nssvbot.protocol.utils.VarIntLongConverter;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class PacketReader {
    
    private final Logger logger = LoggerFactory.getLogger(PacketReader.class);
    private ZlibCompressor zlib = new ZlibCompressor();
    private DataInputStream in;
    private Compression compression;
    private State state;
    
    public void setInput(DataInputStream in) {
        this.in = in;
    }

    public void setCompression(Compression compression) {
        this.compression = compression;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    public Optional<Packet> read() {
        try {
            var length = VarIntLongConverter.readVarInt(in);
            if (compression.isCompressed()) {
                var uncompressedLength = VarIntLongConverter.readVarInt(in);
                if (uncompressedLength.getValue() > 0) {
                    return readCompressedPacket(length.getValue(), uncompressedLength);
                } else {
                    return readNotCompressedPacket(length.getValue() - 1);
                }
            }
            return readNotCompressedPacket(length.getValue());
        } catch (IOException e) {
            throw new ConnectionException("Error while reading a packet from the input stream", e);
        } catch (DataFormatException e) {
            throw new ConnectionException("Error while decompressing data from the input stream", e);
        }
    }
    
    public boolean isAvailable() {
        try {
            return in.available() > 0;
        } catch (IOException e) {
            throw new ConnectionException("Error while checking if there is an available packet in input stream" + e);
        }
    }
    
    private Optional<Packet> readCompressedPacket(int length, ReadField<Integer> uncompressedDataLength) 
            throws IOException, DataFormatException {
        int compressedDataLength = length - uncompressedDataLength.getLength();
        byte[] compressedData = new byte[compressedDataLength];
        compressedData = in.readNBytes(compressedDataLength);
        byte[] data = zlib.decompress(compressedData, uncompressedDataLength.getValue());

        return buildPacket(data);
    }
    
    private Optional<Packet> readNotCompressedPacket(int length) throws IOException {
        byte[] data = in.readNBytes(length);
        return buildPacket(data);
    }
    
    private Optional<Packet> buildPacket(byte[] data) throws IOException {
        Optional<Packet> packet;
        var buffer = new ByteArrayInputStream(data);
        try (var dataStream = new DataInputStream(buffer)) {
            int packetId = VarIntLongConverter.readVarInt(dataStream).getValue();
            if (state == State.LOGIN) {
                packet = buildLoginPacket(packetId, dataStream);
            } else {
                packet = buildPlayPacket(packetId, dataStream);
            }
            
            if (packet.isEmpty()) {
                //logger.info("A packet has been skipped - ID: " + packetId);
            }
        }
        return packet;
    }
    
    private Optional<Packet> buildLoginPacket(int id, DataInputStream data) throws IOException  {
        Packet packet;
        if (id == PacketName.DISCONNECT_LOGIN.getId()) {
            packet = PacketBuilder.disconnectLogin(data);
        } else if (id == PacketName.LOGIN_SUCCESS.getId()) {
            packet = PacketBuilder.loginSuccess(data);
        } else if (id == PacketName.SET_COMPRESSION.getId()) {
            packet = PacketBuilder.setCompression(data);
        } else {
            return Optional.empty();
        }
        
        return Optional.of(packet); 
    }
    
    private Optional<Packet> buildPlayPacket(int id, DataInputStream data) throws IOException  {
        Packet packet;
        if (id == PacketName.JOIN_GAME.getId()) {
            packet = PacketBuilder.joinGame(data);
        } else if (id == PacketName.PLUGIN_MESSAGE_CLIENTBOUND.getId()) {
            packet = PacketBuilder.pluginMessageClientbound(data);
        } else if (id == PacketName.CHAT_MESSAGE_CLIENTBOUND.getId()) {
            packet = PacketBuilder.chatMessageClientbound(data);
        } else if (id == PacketName.KEEP_ALIVE_CLIENTBOUND.getId()) {
            packet = PacketBuilder.keepAliveClientbound(data);
        } else if (id == PacketName.DISCONNECT_PLAY.getId()) {
            packet = PacketBuilder.disconnectPlay(data);
        } else if (id == PacketName.PLAYER_POSITION_AND_LOOK.getId()) {
            packet = PacketBuilder.playerPositionAndLook(data);
        } else if (id == PacketName.RESPAWN.getId()) { 
            packet = PacketBuilder.respawn(data);
        } else {

            int[] ids = {68, 46, 13, 70, 44, 58, 7, 9, 6, 51, 74, 22, 56, 71, 20, 78, 32, 63, 5, 60, 54, 79, 25, 0,
                    62, 67, 72, 76, 50, 33, 38, 40, 39, 34, 30, 24, 27, 19, 49, 52, 73, 77, 65, 64, 12, 11, 18, 10, 2};
            List<Integer> idsl = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                idsl.add(ids[i]);
            }
            if (idsl.contains(id)) {
                return Optional.empty();
            }
            throw new RuntimeException("Skipped packet ID: " + id);
        }
        
        return Optional.of(packet);
    }
}
