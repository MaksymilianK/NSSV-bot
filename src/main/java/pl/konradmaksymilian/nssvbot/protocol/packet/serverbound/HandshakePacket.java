package pl.konradmaksymilian.nssvbot.protocol.packet.serverbound;

import pl.konradmaksymilian.nssvbot.ObjectConstructionException;
import pl.konradmaksymilian.nssvbot.protocol.packet.Packet;
import pl.konradmaksymilian.nssvbot.protocol.packet.PacketName;

public final class HandshakePacket implements Packet {

    private final int protocolVersion;
    private final String serverAddress;
    private final short serverPort;
    private final int nextState;
    
    private HandshakePacket(int protocolVersion, String serverAddress, short serverPort, int nextState) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nextState = nextState;
    }
    
    public int getProtocolVersion() {
        return protocolVersion;
    }

    
    public String getServerAddress() {
        return serverAddress;
    }

    
    public short getServerPort() {
        return serverPort;
    }

    
    public int getNextState() {
        return nextState;
    }

    @Override
    public PacketName getName() {
        return PacketName.HANDSHAKE;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        
        private Integer protocolVersion;
        private String serverAddress;
        private Short serverPort;
        private Integer nextState;
        
        Builder() {}     
        
        public Builder protocolVersion(Integer protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        
        public Builder serverAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        
        public Builder serverPort(Short serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        
        public Builder nextState(Integer nextState) {
            this.nextState = nextState;
            return this;
        }

        public HandshakePacket build() {
            if (protocolVersion == null || serverAddress == null || serverPort == null || nextState == null) {
                throw new ObjectConstructionException("HandshakePacket lacks field");
            }
            return new HandshakePacket(protocolVersion, serverAddress, serverPort, nextState);
        }
    }
}
