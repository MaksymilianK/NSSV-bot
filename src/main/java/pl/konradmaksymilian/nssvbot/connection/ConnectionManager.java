package pl.konradmaksymilian.nssvbot.connection;

import java.util.AbstractMap;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import pl.konradmaksymilian.nssvbot.IllegalMethodInvocationException;
import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.Compression;
import pl.konradmaksymilian.nssvbot.protocol.State;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketReader;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketWriter;
import pl.konradmaksymilian.nssvbot.utils.ZlibCompressor;

public class ConnectionManager {
    
    public static final String HOST = "nssv.pl";
    public static final short PORT = 25565;

    private final PacketReader packetReader;
    private final PacketWriter packetWriter;
    private final Queue<Packet> outgoingPackets = new ConcurrentLinkedQueue<>();
    
    private Runnable onEveryConnection;
    private Runnable onEveryCheckFinish;
    private Consumer<AbstractMap.SimpleImmutableEntry<String, Integer>> onEveryDisconnection;
    private Consumer<Packet> onIncomingPacket;

    private boolean touched;
    private volatile boolean connected;

    public ConnectionManager(PacketReader packetReader, PacketWriter packetWriter) {
        this.packetReader = packetReader;
        this.packetWriter = packetWriter;
        this.touched = false;
        this.connected = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void disconnect() {
        connected = false;
    }
    
    public void setCompression(Compression compression) {
        packetReader.setCompression(compression);
        packetWriter.setCompression(compression);
    }
    
    public void setState(State state) {
        packetReader.setState(state);
    }
    
    public void sendPacket(Packet packet) {
        outgoingPackets.add(packet);
    }
    
    public ConnectionBuilder connectionBuilder() {
        if (touched) {
            throw new IllegalMethodInvocationException("Cannot connect again after connected once");
        }
        return new ConnectionBuilder();
    }
    
    private void doConnect() throws InterruptedException {
        if (touched) {
            throw new IllegalMethodInvocationException("Method connect can be called only once for a single "
                    + "ConnectionManager object");
        } else {
            touched = true;
        }

        var random = new Random();
        while (true) {
            String disconnectionReason = "";

            try (var socket = new SocketWrapper(HOST, PORT)) {
                setUp(socket);
                onEveryConnection.run();
                maintainConnection();
            } catch (ConnectionException e) {
                disconnectionReason = e.getMessage();
            }
            connected = false;
            int delayInSeconds = 180 + random.nextInt(120);
            onEveryDisconnection.accept(new AbstractMap.SimpleImmutableEntry<>(disconnectionReason, delayInSeconds));
            Thread.sleep(delayInSeconds * 1000);
        }
    }
    
    private void setUp(SocketWrapper socket) {
        connected = true;
        
        packetReader.setInput(socket.getIn());
        packetWriter.setOutput(socket.getOut());
    }
    
    private void maintainConnection() throws InterruptedException {
        while (connected) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Connection has been interrupted");
            }
            
            listenToIncomingPackets();
            passPackets();
            Thread.sleep(500);
            onEveryCheckFinish.run();
        }
    }
    
    private void listenToIncomingPackets() {
        while (packetReader.isAvailable()) {
            var optionalPacket = packetReader.read();
            if (optionalPacket.isPresent()) {
                onIncomingPacket.accept(optionalPacket.get());
            }
        }
    }
    
    private void passPackets() {
        Packet packet;
        while ((packet = outgoingPackets.poll()) != null) {
            packetWriter.write(packet);
        }
    }

    public class ConnectionBuilder {
        
        ConnectionBuilder() {}
        
        public ConnectionBuilder onEveryConnection(Runnable onConnection) {
            if (touched) {
                throw new IllegalMethodInvocationException("Cannot change the ConnectionManager since the connection "
                        + "starts");
            }
            onEveryConnection = onConnection;
            return this;
        }
        
        public ConnectionBuilder onEveryDisconnection(
                Consumer<AbstractMap.SimpleImmutableEntry<String, Integer>> onDisconnection) {
            if (touched) {
                throw new IllegalMethodInvocationException("Cannot change the ConnectionManager since the connection "
                        + "starts");
            }
            onEveryDisconnection = onDisconnection;
            return this;
        }
        
        public ConnectionBuilder onEveryCheckFinish(Runnable onEveryCheck) {
            if (touched) {
                throw new IllegalMethodInvocationException("Cannot change the ConnectionManager since the connection "
                        + "starts");
            }
            onEveryCheckFinish = onEveryCheck;
            return this;
        }
        
        public ConnectionBuilder onIncomingPacket(Consumer<Packet> onPacket) {
            if (touched) {
                throw new IllegalMethodInvocationException("Cannot change the ConnectionManager since the connection "
                        + "starts");
            }
            onIncomingPacket = onPacket;
            return this;
        }
        
        public void connect() throws InterruptedException {
            if (onEveryConnection == null || onEveryDisconnection == null || onEveryCheckFinish == null 
                    || onIncomingPacket == null) {
                throw new ObjectConstructionException("Connection manager has not been properly initialized");
            }
            doConnect();
        }
    }
}
