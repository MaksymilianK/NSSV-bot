package pl.konradmaksymilian.nssvbot.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.LoginSuccessPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.clientbound.SetCompressionPacket;

public class PacketReaderTest {
    
    private PacketReader packetReader;
    
    @BeforeEach
    public void setUp() {
        packetReader = new PacketReader();
    }
    
    @Test
    public void readingSetCompressionPacket() {
        packetReader.setCompression(new Compression(false, Integer.MAX_VALUE));
        packetReader.setState(State.LOGIN);
        byte[] packetBytes = {3, 3, -128, 2};
        var in = new ByteArrayInputStream(packetBytes);
        packetReader.setInput(new DataInputStream(in));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.SET_COMPRESSION);
        assertThat(((SetCompressionPacket) packet).getThreshold()).isEqualTo(256);
    }
    
    @Test
    public void readingLoginSuccessPacket() {
        packetReader.setCompression(new Compression(true, 256));
        packetReader.setState(State.LOGIN);
        byte[] packetBytes = {20, 0, 2, 3, 0x31, 0x2d, 0x33, 13, 0x47, 0x65, 0x6e, 0x69, 0x75, 0x73, 0x7a, 0x4d, 0x69, 
                0x73, 0x74, 0x72, 0x7a};
        var in = new ByteArrayInputStream(packetBytes);
        packetReader.setInput(new DataInputStream(in));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket).isPresent();
        var packet = readPacket.get();
        assertThat(packet.getName()).isEqualTo(PacketName.LOGIN_SUCCESS);
        var loginSuccess = (LoginSuccessPacket) packet;
        assertThat(loginSuccess.getUUID()).isEqualTo("1-3");
        assertThat(loginSuccess.getUsername()).isEqualTo("GeniuszMistrz");
    }
}
