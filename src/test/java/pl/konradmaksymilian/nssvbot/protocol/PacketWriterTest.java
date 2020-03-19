package pl.konradmaksymilian.nssvbot.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pl.konradmaksymilian.nssvbot.protocol.packet.PacketWriter;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ChatMessageServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.ClientSettingsPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.KeepAliveServerboundPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.LoginStartPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.TeleportConfirmPacket;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class PacketWriterTest {

    private PacketWriter packetWriter;
    
    @Mock
    private ZlibCompressor zlib;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        packetWriter = new PacketWriter(zlib);
        
        packetWriter.setCompression(new Compression(false, Integer.MAX_VALUE));
    }

    @Test
    public void compressIfCompressionSetToTrueAndLengthGraterThanTreshold() throws IOException {
        packetWriter.setCompression(new Compression(true, 10));
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        byte[] compressedBytes = {0x00, 0x01, 0x02}; //does not make any sense - just for stubbing
        when(zlib.compress(any())).thenReturn(compressedBytes);
        
        packetWriter.write(new ChatMessageServerboundPacket("abcdefghi"));
        
        assertThat(out.toByteArray()).containsExactly(4, 11, 0, 1, 2);
    }

    @Test
    public void doNotCompressIfCompressionSetToTrueAndLengthLessThanTreshold() throws IOException {
        packetWriter.setCompression(new Compression(true, 12));
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(new ChatMessageServerboundPacket("abcdefghi"));
        
        assertThat(out.toByteArray()).containsExactly(0x0c, 0x00, 0x02, 0x09, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
                0x68, 0x69);
    }
    
    @Test
    public void writingHandshakePacket() {
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(HandshakePacket.builder()
                .protocolVersion(340)
                .serverAddress("nssv.pl")
                .serverPort((short) 25565)
                .nextState(2)
                .build());
        
        assertThat(out.toByteArray()).containsExactly(0x0e, 0x00, -0x2c, 0x2, 0x07, 0x6e, 0x73, 0x73, 0x76, 0x2e, 0x70,
                0x6c, 0x63, -0x23, 0x02);
    }
    
    @Test
    public void writingLoginStartPacket() {
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(new LoginStartPacket("GeniuszMistrz"));
        
        assertThat(out.toByteArray()).containsExactly(0x0f, 0x00, 0x0d, 0x47, 0x65, 0x6e, 0x69, 0x75, 0x73, 0x7a, 0x4d, 
                0x69, 0x73, 0x74, 0x72, 0x7a);
    }
    
    @Test
    public void writingKeepAlivePacket() {
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(new KeepAliveServerboundPacket(12L));
        
        assertThat(out.toByteArray()).containsExactly(0x09, 0x0b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0c);
    }
    
    @Test
    public void writingChatMessagePacket() {
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(new ChatMessageServerboundPacket("abc"));
        
        assertThat(out.toByteArray()).containsExactly(0x05, 0x02, 0x03, 0x61, 0x62, 0x63);
    }
    
    @Test
    public void writingTeleportConfirmPacket() {
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(new TeleportConfirmPacket(123));
        
        assertThat(out.toByteArray()).containsExactly(0x02, 0x00, 0x7b);
    }
    
    @Test
    public void writingClientSettingsPacket() {
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(ClientSettingsPacket.builder()
                .locale("pl_PL")
                .viewDistance(2)
                .chatMode(0)
                .chatColours(true)
                .displayedSkinParts(0x00)
                .mainHand(1)
                .build());
        
        assertThat(out.toByteArray()).containsExactly(0x0c, 0x04, 0x05, 0x70, 0x6C, 0x5F, 0x50, 0x4C, 0x02, 0x00, 0x01,
                0x00, 0x01);
    }
}
