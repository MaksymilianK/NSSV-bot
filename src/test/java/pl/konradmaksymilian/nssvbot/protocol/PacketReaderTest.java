package pl.konradmaksymilian.nssvbot.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.zip.DataFormatException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.ChatMessageClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectLoginPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.DisconnectPlayPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.KeepAliveClientboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.LoginSuccessPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.PlayerPositionAndLookPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.RespawnPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.SetCompressionPacket;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class PacketReaderTest {
    
    @Mock
    private ZlibCompressor zlibCompressor;
    
    private PacketReader packetReader;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        packetReader = new PacketReader(zlibCompressor);
        
        packetReader.setCompression(new Compression(false, Integer.MAX_VALUE));
    }
    
    @Test
    public void decompressIfCompressionSetToTrueAndUncompressedLengthNot0() throws DataFormatException {
        packetReader.setCompression(new Compression(true, 1));
        packetReader.setState(State.LOGIN);
        byte[] compressedData = {0x02, 0x54, 0x53, 0x14}; //these bytes do not make much sense, just for stubbing
        byte[] packetBytes = {0x05, 0x0a, 0x02, 0x54, 0x53, 0x14}; //size bytes + compressedData
        byte[] decompressedBytes = {0x02, 0x03, 0x31, 0x32, 0x33, 0x04, 0x31, 0x32, 0x33, 0x34};
        when(zlibCompressor.decompress(compressedData, 10)).thenReturn(decompressedBytes);
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.LOGIN_SUCCESS);
        var loginSuccess = (LoginSuccessPacket) packet;
        assertThat(loginSuccess.getUUID()).isEqualTo("123");
        assertThat(loginSuccess.getUsername()).isEqualTo("1234");
    }
    
    @Test
    public void doNotDecompressIfCompressionSetToTrueAndUncompressedLength0() throws DataFormatException {
        packetReader.setCompression(new Compression(true, 10));
        packetReader.setState(State.LOGIN);
        byte[] data = {0x02, 0x03, 0x31, 0x32, 0x33, 0x01, 0x31}; //these bytes do not make much sense, just for stubbing
        byte[] packetBytes = {0x08, 0x00, 0x02, 0x03, 0x31, 0x32, 0x33, 0x01, 0x31}; //size bytes + data
        when(zlibCompressor.decompress(data, 10)).thenThrow(new RuntimeException("This method should not be invoked"));
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.LOGIN_SUCCESS);
        var loginSuccess = (LoginSuccessPacket) packet;
        assertThat(loginSuccess.getUUID()).isEqualTo("123");
        assertThat(loginSuccess.getUsername()).isEqualTo("1");
    }
    
    @Test
    public void readingSetCompressionPacket() {
        packetReader.setState(State.LOGIN);
        byte[] packetBytes = {3, 3, -128, 2};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.SET_COMPRESSION);
        assertThat(((SetCompressionPacket) packet).getThreshold()).isEqualTo(256);
    }
    
    @Test
    public void readingLoginSuccessPacket() {
        packetReader.setState(State.LOGIN);
        byte[] packetBytes = {0x13, 0x02, 0x03, 0x31, 0x2d, 0x33, 0x0d, 0x47, 0x65, 0x6e, 0x69, 0x75, 0x73, 0x7a, 0x4d, 0x69, 
                0x73, 0x74, 0x72, 0x7a};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.LOGIN_SUCCESS);
        var loginSuccess = (LoginSuccessPacket) packet;
        assertThat(loginSuccess.getUUID()).isEqualTo("1-3");
        assertThat(loginSuccess.getUsername()).isEqualTo("GeniuszMistrz");
    }
    
    @Test
    public void readingDisconnectLoginPacket() {
        packetReader.setState(State.LOGIN);
        byte[] packetBytes = {0x10, 0x00, 0x0e, 0x7b, 0x22, 0x74, 0x65, 0x78, 0x74, 0x22, 0x3a, 0x22, 0x61, 0x62, 0x63, 
                0x22, 0x7d};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.DISCONNECT_LOGIN);
        assertThat(((DisconnectLoginPacket) packet).getReason().toString()).isEqualTo("abc");
    }
    
    @Test
    public void readingDisconnectPlayPacket() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {0x10, 0x1a, 0x0e, 0x7b, 0x22, 0x74, 0x65, 0x78, 0x74, 0x22, 0x3a, 0x22, 0x61, 0x62, 0x63, 
                0x22, 0x7d};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.DISCONNECT_PLAY);
        assertThat(((DisconnectPlayPacket) packet).getReason().toString()).isEqualTo("abc");
    }
    
    @Test
    public void readingJoinGamePacket() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {0x13, 0x23, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x66,
                0x6c, 0x61, 0x74, 0x00};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        assertThat(readPacket.get().getName()).isEqualTo(PacketName.JOIN_GAME);
    }
    
    @Test
    public void readingKeepAlivePacket() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {0x09, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.KEEP_ALIVE_CLIENTBOUND);
        assertThat(((KeepAliveClientboundPacket) packet).getKeepAliveId()).isEqualTo(15L);
    }
    
    @Test
    public void readingPlayerPositionAndLookPacket() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {0x23, 0x2f, 0x40, 0x5f, 0x60, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x5f, 0x60, 0x00, 0x00, 
                0x00, 0x00, 0x00, 0x40, 0x5f, 0x60, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43, 0x55, 0x66, 0x66, 0x43, 0x55, 
                0x66, 0x66, 0x00, 0x03};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.PLAYER_POSITION_AND_LOOK);
        assertThat(((PlayerPositionAndLookPacket) packet).getTeleportId()).isEqualTo(3);
    }
    
    @Test
    public void readingRespawnPacket() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {0x0c, 0x35, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x04, 0x66, 0x6c, 0x61, 0x74};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.RESPAWN);
        var respawn = (RespawnPacket) packet;
        assertThat(respawn.getDimension()).isEqualTo(0);
        assertThat(respawn.getGamemode()).isEqualTo(1);
    }
    
    @Test
    public void readingChatMessagePacket() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {0x11, 0x0f, 0x0f, 0x7B, 0x22, 0x74, 0x65, 0x78, 0x74, 0x22, 0x3A, 0x20, 0x22, 0x61, 0x62, 
                0x63, 0x22, 0x7D, 0x00};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.CHAT_MESSAGE_CLIENTBOUND);
        var message = ((ChatMessageClientboundPacket) packet).getMessage();
        assertThat(message.getComponents()).hasSize(1);
        assertThat(message.getComponents().get(0).getText()).isEqualTo("abc");
    }
    
    @Test
    public void readingComplicatedChatMessage() {
        packetReader.setState(State.PLAY);
        byte[] packetBytes = {-0x3a, 0x01, 0x0f, -0x3e, 0x01, 0x7B, 0x22, 0x74, 0x65, 0x78, 0x74, 0x22, 0x3A, 0x22, 0x61, 0x62, 0x63, 0x22, 0x2C, 0x22, 
                0x62, 0x6F, 0x6C, 0x64, 0x22, 0x3A, 0x74, 0x72, 0x75, 0x65, 0x2C, 0x22, 0x69, 0x74, 0x61, 0x6C, 0x69, 
                0x63, 0x22, 0x3A, 0x66, 0x61, 0x6C, 0x73, 0x65, 0x2C, 0x22, 0x63, 0x6F, 0x6C, 0x6F, 0x72, 0x22, 0x3A, 
                0x22, 0x72, 0x65, 0x64, 0x22, 0x2C, 0x22, 0x65, 0x78, 0x74, 0x72, 0x61, 0x22, 0x3A, 0x5B, 0x7B, 0x22, 
                0x74, 0x65, 0x78, 0x74, 0x22, 0x3A, 0x22, 0x64, 0x65, 0x66, 0x22, 0x2C, 0x22, 0x62, 0x6F, 0x6C, 0x64, 
                0x22, 0x3A, 0x66, 0x61, 0x6C, 0x73, 0x65, 0x2C, 0x22, 0x75, 0x6E, 0x64, 0x65, 0x72, 0x6C, 0x69, 0x6E, 
                0x65, 0x64, 0x22, 0x3A, 0x74, 0x72, 0x75, 0x65, 0x2C, 0x22, 0x69, 0x74, 0x61, 0x6C, 0x69, 0x63, 0x22, 
                0x3A, 0x74, 0x72, 0x75, 0x65, 0x2C, 0x22, 0x65, 0x78, 0x74, 0x72, 0x61, 0x22, 0x3A, 0x5B, 0x7B, 0x22, 
                0x74, 0x65, 0x78, 0x74, 0x22, 0x3A, 0x22, 0x67, 0x68, 0x69, 0x22, 0x2C, 0x22, 0x69, 0x74, 0x61, 0x6C, 
                0x69, 0x63, 0x22, 0x3A, 0x66, 0x61, 0x6C, 0x73, 0x65, 0x2C, 0x22, 0x63, 0x6F, 0x6C, 0x6F, 0x72, 0x22, 
                0x3A, 0x22, 0x77, 0x68, 0x69, 0x74, 0x65, 0x22, 0x7D, 0x2C, 0x22, 0x6A, 0x6B, 0x6C, 0x22, 0x5D, 0x7D, 
                0x2C, 0x22, 0x6D, 0x6E, 0x6F, 0x22, 0x5D, 0x7D, 0x0A, 0x00};
        packetReader.setInput(new DataInputStream(new ByteArrayInputStream(packetBytes)));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.CHAT_MESSAGE_CLIENTBOUND);
        var components = ((ChatMessageClientboundPacket) packet).getMessage().getComponents();
        assertThat(components).hasSize(5);
        assertThat(components.get(0).getText()).isEqualTo("abc");
        assertThat(components.get(0).getStyle().getColour()).isPresent();
        assertThat(components.get(0).getStyle().getColour().get()).isEqualTo("red");
        assertThat(components.get(0).getStyle().isBold()).isTrue();
        assertThat(components.get(0).getStyle().isItalic()).isFalse();
        assertThat(components.get(0).getStyle().isObfuscated()).isFalse();
        assertThat(components.get(0).getStyle().isStrikethrough()).isFalse();
        assertThat(components.get(0).getStyle().isUnderlined()).isFalse();
        assertThat(components.get(1).getText()).isEqualTo("def");
        assertThat(components.get(1).getStyle().getColour()).isPresent();
        assertThat(components.get(1).getStyle().getColour().get()).isEqualTo("red");
        assertThat(components.get(1).getStyle().isBold()).isFalse();
        assertThat(components.get(1).getStyle().isItalic()).isTrue();
        assertThat(components.get(1).getStyle().isObfuscated()).isFalse();
        assertThat(components.get(1).getStyle().isStrikethrough()).isFalse();
        assertThat(components.get(1).getStyle().isUnderlined()).isTrue();
        assertThat(components.get(2).getText()).isEqualTo("ghi");
        assertThat(components.get(2).getStyle().getColour()).isPresent();
        assertThat(components.get(2).getStyle().getColour().get()).isEqualTo("white");
        assertThat(components.get(2).getStyle().isBold()).isFalse();
        assertThat(components.get(2).getStyle().isItalic()).isFalse();
        assertThat(components.get(2).getStyle().isObfuscated()).isFalse();
        assertThat(components.get(2).getStyle().isStrikethrough()).isFalse();
        assertThat(components.get(2).getStyle().isUnderlined()).isTrue();
        assertThat(components.get(3).getText()).isEqualTo("jkl");
        assertThat(components.get(3).getStyle().getColour()).isPresent();
        assertThat(components.get(3).getStyle().getColour().get()).isEqualTo("red");
        assertThat(components.get(3).getStyle().isBold()).isFalse();
        assertThat(components.get(3).getStyle().isItalic()).isTrue();
        assertThat(components.get(3).getStyle().isObfuscated()).isFalse();
        assertThat(components.get(3).getStyle().isStrikethrough()).isFalse();
        assertThat(components.get(3).getStyle().isUnderlined()).isTrue();
        assertThat(components.get(4).getText()).isEqualTo("mno");
        assertThat(components.get(4).getStyle().getColour()).isPresent();
        assertThat(components.get(4).getStyle().getColour().get()).isEqualTo("red");
        assertThat(components.get(4).getStyle().isBold()).isTrue();
        assertThat(components.get(4).getStyle().isItalic()).isFalse();
        assertThat(components.get(4).getStyle().isObfuscated()).isFalse();
        assertThat(components.get(4).getStyle().isStrikethrough()).isFalse();
        assertThat(components.get(4).getStyle().isUnderlined()).isFalse();
    }
}
