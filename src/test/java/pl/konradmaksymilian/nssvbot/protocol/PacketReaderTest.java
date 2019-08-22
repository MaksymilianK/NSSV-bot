package pl.konradmaksymilian.nssvbot.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.konradmaksymilian.nssvbot.protocol.packet.LoginSuccessPacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.SetCompressionPacket;

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
        byte[] packet = {3, 3, -128, 2};
        var in = new ByteArrayInputStream(packet);
        packetReader.setInput(new DataInputStream(in));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket.getName()).isEqualTo(PacketName.SET_COMPRESSION);
        assertThat(((SetCompressionPacket) readPacket).getThreshold()).isEqualTo(256);
    }
    
    @Test
    public void readingLoginSuccessPacket() {
        packetReader.setCompression(new Compression(true, 256));
        packetReader.setState(State.LOGIN);
        byte[] packet = {20, 0, 2, 3, 0x31, 0x2d, 0x33, 13, 0x47, 0x65, 0x6e, 0x69, 0x75, 0x73, 0x7a, 0x4d, 0x69, 0x73,
                0x74, 0x72, 0x7a};
        var in = new ByteArrayInputStream(packet);
        packetReader.setInput(new DataInputStream(in));
        
        var readPacket = packetReader.read();
        
        assertThat(readPacket.getName()).isEqualTo(PacketName.LOGIN_SUCCESS);
        var loginSuccess = (LoginSuccessPacket) readPacket;
        assertThat(loginSuccess.getUUID()).isEqualTo("1-3");
        assertThat(loginSuccess.getUsername()).isEqualTo("GeniuszMistrz");
    }
}
