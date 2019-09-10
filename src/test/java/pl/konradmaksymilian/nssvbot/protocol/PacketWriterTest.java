package pl.konradmaksymilian.nssvbot.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.konradmaksymilian.nssvbot.protocol.packet.PacketWriter;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.HandshakePacket;
import pl.konradmaksymilian.nssvbot.protocol.packet.serverbound.LoginStartPacket;

public class PacketWriterTest {

    private PacketWriter packetWriter;
    
    @BeforeEach
    public void setUp() {
        packetWriter = new PacketWriter();
    }
    
    @Test
    public void writingHandshake() {
        packetWriter.setCompression(new Compression(false, Integer.MAX_VALUE));
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(HandshakePacket.builder()
                .protocolVersion(340)
                .serverAddress("nssv.pl")
                .serverPort((short) 25565)
                .nextState(2) //means LOGIN
                .build());
        
        assertThat(out.toByteArray()).containsExactly(14, 0, -44, 2, 7, 0x6e, 0x73, 0x73, 0x76, 0x2e, 0x70, 0x6c, 99, 
                -35, 2);
    }
    
    @Test
    public void writingLoginStart() {
        packetWriter.setCompression(new Compression(false, Integer.MAX_VALUE));
        var out = new ByteArrayOutputStream();
        packetWriter.setOutput(new DataOutputStream(out));
        
        packetWriter.write(new LoginStartPacket("GeniuszMistrz"));
        
        assertThat(out.toByteArray()).containsExactly(15, 0, 13, 0x47, 0x65, 0x6e, 0x69, 0x75, 0x73, 0x7a, 0x4d, 0x69, 
                0x73, 0x74, 0x72, 0x7a);
    }
}
