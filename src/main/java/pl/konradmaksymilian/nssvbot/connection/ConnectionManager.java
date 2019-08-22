package pl.konradmaksymilian.nssvbot.connection;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.PacketWriter;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;
import pl.konradmaksymilian.nssvbot.protocol.packet.SetCompressionPacket;

public class ConnectionManager {
    
    public static final String HOST = "nssv.pl";
    public static final short PORT = 25565;

    private boolean connected = false;
    private final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private final PacketReader packetReader = new PacketReader();
    private final PacketWriter packetWriter = new PacketWriter();
    private final Queue<Packet> outgoingPackets = new ConcurrentLinkedQueue<>();
    private Consumer<Packet> onIncomingPacket;
    
    public ConnectionManager(Consumer<Packet> onIncomingPacket) {
        this.onIncomingPacket = onIncomingPacket;
    }
    
    public void connect() throws InterruptedException {
        if (connected) {
            throw new IllegalMethodInvocationException("Method connect can be called only once for a single ConnectionManager "
                    + "object");
        } else {
            connected = true;
        }
            
        var random = new Random();
        do {
            try (var connection = new Connection(HOST, PORT)) {
                setUp(connection);
                while (true) {
                    listenToIncomingPackets();
                    passPackets();
                    Thread.sleep(100);
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                logger.info("Reconnecting to the server...");
                Thread.sleep(300000 + random.nextInt(180000)); 
            }
        } while (true);
    }

    public void sendPacket(Packet packet) {
        outgoingPackets.add(packet);
    }
    
    private void setUp(Connection connection) {
        logger.info("Connected to the server!");
        packetReader.setInput(connection.getIn());
        packetWriter.setOutput(connection.getOut());
        setCompression(new Compression(false, Integer.MAX_VALUE));
        packetReader.setState(State.HANDSHAKING);
    }
    
    private void listenToIncomingPackets() {
        while (packetReader.isAvailable()) {
            var packet = packetReader.read();
            if (packet.getName().equals(PacketName.SET_COMPRESSION)) {
                int treshold = ((SetCompressionPacket) packet).getThreshold();
                setCompression(new Compression(true, treshold));
            } else {
                if (packet.getName().equals(PacketName.LOGIN_SUCCESS)) {
                    packetReader.setState(State.PLAY);
                }
                onIncomingPacket.accept(packet);
            }
        }
    }
    
    private void passPackets() {
        Packet packet;
        while ((packet = outgoingPackets.poll()) != null) {
            packetWriter.write(packet);
            if (packet.getName().equals(PacketName.HANDSHAKE)) {
                packetReader.setState(State.LOGIN);
            }
        }
    }
    
    private void setCompression(Compression compression) {
        packetReader.setCompression(compression);
        packetWriter.setCompression(compression);
    }
}
